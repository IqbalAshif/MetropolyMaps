package com.example.routetracker

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.scale
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.routetracker.helpers.*
import com.example.routetracker.sensors.StepSensor
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.location.NominatimPOIProvider
import org.osmdroid.bonuspack.location.POI
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapAdapter
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import kotlin.math.log
import kotlin.math.roundToInt


class MapFragment : Fragment(), LocationListener {

    private lateinit var lm: LocationManager
    private lateinit var stepSensor: StepSensor

    private lateinit var stepCount: TextView

    lateinit var map: MapView
    private lateinit var marker: Marker
    private lateinit var path: Polyline

    private var pois: MutableList<POI> = mutableListOf()

    private lateinit var toggle: FloatingActionButton
    private lateinit var info: FloatingActionButton

    // Animations
    private lateinit var appear: Animation
    private lateinit var disappear: Animation
    private lateinit var rotateclock: Animation
    private lateinit var rotateanticlock: Animation


    private var panning = false

    companion object {
        fun newInstance() = MapFragment()
    }

    val userAgent = BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME


    /* UI */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        super.onCreate(savedInstanceState)
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))

        // Stepcounter
        stepCount = view.findViewById<TextView>(R.id.steps)
        stepSensor = StepSensor(context)
        stepSensor.onStopped = { stepCount.text = "" }
        stepSensor.onTriggered = {
            if (stepCount.tag != null && toggle.tag == true)
                stepCount.text = (stepCount.text.toString().ifEmpty { "0" }.ifBlank { "0" }
                    .toFloat() + it.values[0] - (stepCount.tag as Float)).roundToInt().toString()

            stepCount.tag = it.values[0]
        }


        // Map
        lm = this.context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        map = view.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.isTilesScaledToDpi = true
        map.setMultiTouchControls(true)
        map.controller.setZoom(3.0)
        map.setOnTouchListener { _, _ -> run { panning = true; false } }
        map.addMapListener(object : MapAdapter() {
            override fun onZoom(event: ZoomEvent?): Boolean {
                if (event != null && pois.isNotEmpty())
                    if (event.zoomLevel >= 18)
                        refreshPointsOfInterest(true) // Use tile icons when they appear
                    else
                        refreshPointsOfInterest(false)
                return super.onZoom(event)
            }
        })

        createOverlays()

        // Gps Fab
        toggle = view.findViewById<FloatingActionButton>(R.id.toggle)
        toggle.tag = false // Recording?
        toggle.backgroundTintList = ColorStateList.valueOf(Color.GREEN + Color.GREEN * 40 / 100)
        toggle.setOnClickListener {
            if (toggle.tag == false && requestLocationPermissions(requireActivity()))
                enableGps() // Start recording
            else
                disableGps() // Stop recording
        }

        // Info Fab
        info = view.findViewById<FloatingActionButton>(R.id.info)
        info.setOnClickListener {


            parentFragmentManager.beginTransaction().hide(this)
                .add(R.id.fragmentContainerView, DashboardFragment.newInstance(this))
                .addToBackStack("")
                .commit()

        }

        // Animations
        appear = AnimationUtils.loadAnimation(context, R.anim.appear)
        disappear = AnimationUtils.loadAnimation(context, R.anim.disappear)
        rotateclock = AnimationUtils.loadAnimation(context, R.anim.rotate_clock)
        rotateanticlock = AnimationUtils.loadAnimation(context, R.anim.rotate_anticlock)

        // Intents
        // Location can be shown calling the following:
        //val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:37.7749,-122.4194"))
        //startActivity(mapIntent)

        // Intent
        if (activity != null && requireActivity().intent.data != null) {
            map.controller.setCenter(parseLocation(requireActivity().intent.data.toString()))
            map.controller.setZoom(18.0)
        }




        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        lm.removeUpdates(this)
    }

    override fun onPause() {
        super.onPause()
        stepSensor.disable()
    }

    override fun onResume() {
        super.onResume()
        stepSensor.enable()
    }


    /* MAP */
    private fun createOverlays() {

        // Path
        path = Polyline(map, true)
        path.outlinePaint.color = Color.parseColor("#73B9FF")
        path.infoWindow = null
        map.overlays.add(path)

        // Position
        marker = Marker(map)
        marker.setOnMarkerClickListener { _, _ -> panning = false; true }
        marker.icon =
            AppCompatResources.getDrawable(this.requireContext(), R.drawable.ic_baseline_position)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.alpha = 0f
        map.overlays.add(marker)
    }

    fun fetchPointsOfInterest(request: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val poiProvider = NominatimPOIProvider(userAgent)
            try {
                pois = poiProvider.getPOIInside(map.boundingBox, request, 30)
                Log.d("Points of Interest", pois.size.toString())
                CoroutineScope(Dispatchers.Main).launch {
                    refreshPointsOfInterest()
                }
            } catch (exp: NullPointerException) { // Network Error
                Log.e("Network Error", "Failed to fetch points of interest")
            }
        }
    }

    private fun refreshPointsOfInterest(invisible: Boolean = false) {
        CoroutineScope(Dispatchers.Unconfined).launch {
            val overlays: MutableList<Overlay> = mutableListOf()
            map.overlays.removeAll { it != marker && it != path } // Remove old overlays if any exist
            pois.forEach {
                val infoWindow = MarkerWindow(map)
                val poimarker = Marker(map)
                poimarker.icon = BitmapDrawable(resources, it.mThumbnail.scale(100, 100))
                poimarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                poimarker.position = it.mLocation
                poimarker.isFlat = true
                infoWindow.seTitle(it.mDescription.takeWhile { it != ',' })
                poimarker.infoWindow = infoWindow
                poimarker.relatedObject = it
                poimarker.closeInfoWindow()
                if (invisible) poimarker.alpha = 0f
                overlays.add(poimarker)
               // poimarker.setOnMarkerClickListener { _, _ -> panning = false; true }
            }

            CoroutineScope(Dispatchers.Main).launch {
                map.overlays.addAll(overlays)
                map.invalidate()
            }

            parentFragmentManager.popBackStack()
        }
    }

    /* Location */
    @SuppressLint("MissingPermission")
    private fun enableGps() {
        if (locationProvider(requireContext()) != null) {
            toggle.tag = true
            toggle.backgroundTintList = ColorStateList.valueOf(Color.RED + Color.RED * 40 / 100)
            lm.requestLocationUpdates(locationProvider(requireContext())!!, 1000, 15f, this)
        } else
            requestLocationPermissions(requireActivity())
    }

    private fun disableGps(animation: Boolean = true) {
        toggle.tag = false

        if (animation) {
            info.startAnimation(disappear)
            toggle.startAnimation(rotateanticlock)
            toggle.setImageResource(R.drawable.ic_baseline_locationoff)
        }

        // Stop recording
        toggle.backgroundTintList =
            ColorStateList.valueOf(Color.GREEN + Color.GREEN * 40 / 100)
        lm.removeUpdates(this)

        // Clear map
        path.setPoints(listOf())
        marker.alpha = 0f
        marker.closeInfoWindow()
        map.invalidate()

        // Stop stepcounter
        stepSensor.disable()
    }


    override fun onProviderEnabled(provider: String) {
        if(toggle.tag == false)
            toggle.setImageResource(R.drawable.ic_baseline_locationoff)
        else {
            toggle.startAnimation(rotateclock)
            toggle.setImageResource(R.drawable.ic_baseline_location)
        }

    }

    override fun onProviderDisabled(provider: String) {
        Toast.makeText(
            this.context,
            "Please enable $provider",
            Toast.LENGTH_LONG
        ).show()

        toggle.startAnimation(rotateanticlock)
        toggle.setImageResource(R.drawable.ic_baseline_locationdisabled)
    }

    override fun onLocationChanged(p0: Location) {
        Log.d("GEOLOCATION", "new latitude: ${p0.latitude} and longitude: ${p0.longitude}")

        val point = GeoPoint(p0.latitude, p0.longitude)

        // Marker
        marker.position = point
        marker.title =
            point.longitude.roundToDecimal(5).toString() + ',' + point.latitude.roundToDecimal(5)
                .toString()

        CoroutineScope(Dispatchers.IO).launch {
            marker.subDescription = getAddress(context, point)
        }


        if (path.actualPoints.isEmpty()) // First location
        {
            info.startAnimation(appear)
            marker.alpha = 1f


            toggle.startAnimation(rotateclock)
            toggle.setImageResource(R.drawable.ic_baseline_location)

            map.controller.setZoom(18.0)
            map.controller.setCenter(point)

            // Data
            stepSensor.enable()
        } else if (!panning) map.controller.setCenter(point)

        // Path
        path.addPoint(point)

        map.invalidate()
    }

}
