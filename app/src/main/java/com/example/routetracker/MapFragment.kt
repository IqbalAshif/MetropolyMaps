package com.example.routetracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.preference.PreferenceManager
import com.example.routetracker.helpers.locationProvider
import com.example.routetracker.helpers.requestLocationPermissions
import com.example.routetracker.helpers.roundToDecimal
import org.osmdroid.config.Configuration

import com.example.routetracker.sensors.StepSensor
import kotlin.math.roundToInt


class MapFragment : Fragment(), LocationListener {

    private lateinit var lm: LocationManager

    private lateinit var stepSensor: StepSensor
    private lateinit var stepCount: TextView
    var stepsTotal: Float? = null // Alltime stepcount

    lateinit var map: MapView
    private lateinit var marker: Marker
    private lateinit var path: Polyline

    private lateinit var toggle: FloatingActionButton
    private lateinit var info: FloatingActionButton

    // Animations
    private lateinit var appear: Animation
    private lateinit var disappear: Animation

    companion object {
        fun newInstance() = MapFragment()
    }

    /* UI */
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
        stepSensor.onSensorStopped = { stepCount.text = ""}
        stepSensor.onSensorTriggered = {
            if(stepCount.tag != null && toggle.tag == true)
                stepCount.text = (stepCount.text.toString().ifEmpty { "0" }.ifBlank { "0" }.toFloat() + it.values[0] - (stepCount.tag as Float)).roundToInt().toString()

            stepCount.tag = it.values[0]
        }

        // Map
        lm = this.context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        map = view.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.isTilesScaledToDpi = true
        map.setMultiTouchControls(true)
        map.controller.setZoom(3.0)
        createOverlays()

        // Gps Fab
        toggle = view.findViewById<FloatingActionButton>(R.id.toggle)
        toggle.tag = false // Recording?
        toggle.backgroundTintList = ColorStateList.valueOf(Color.GREEN + Color.GREEN * 40 / 100)
        toggle.setOnClickListener{
            if (toggle.tag == false && requestLocationPermissions(requireActivity()))
                enableGps() // Start recording
            else
                disableGps() // Stop recording
        }

        // Info Fab
        info = view.findViewById<FloatingActionButton>(R.id.info)
        info.setOnClickListener {
            parentFragmentManager.beginTransaction().hide(this)
                .add(R.id.fragmentContainerView, DashboardFragment.newInstance())
                .addToBackStack("")
                .commit()
        }

        // Animations
        appear = AnimationUtils.loadAnimation(context, R.anim.appear)
        disappear = AnimationUtils.loadAnimation(context, R.anim.disappear)

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        lm.removeUpdates(this)
        stepSensor.disable()
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
        path.outlinePaint.color = Color.RED
        path.infoWindow = null
        map.overlays.add(path)

        // Person
        marker = Marker(map)
        marker.icon = AppCompatResources.getDrawable(this.requireContext(), R.drawable.person)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.alpha = 0f
        map.overlays.add(marker)
    }

    /* Location */
    @SuppressLint("MissingPermission")
    private fun enableGps() {
        if (locationProvider(requireContext()) != null) {
            stepSensor.enable()
            toggle.tag = true
            toggle.backgroundTintList =
                ColorStateList.valueOf(Color.RED + Color.RED * 40 / 100)
            lm.requestLocationUpdates(locationProvider(requireContext())!!, 1000, 15f, this)
        } else {
            Log.e("Location", "Insufficient permissions, location needed")
            Toast.makeText(
                this.context?.applicationContext,
                "Insufficient permissions, location needed",
                Toast.LENGTH_LONG
            ).show()

            requestLocationPermissions(requireActivity())
        }
    }

    private fun disableGps(animation: Boolean = true) {
        toggle.tag = false

        if (animation) info.startAnimation(disappear)

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


    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        Toast.makeText(
            this.context,
            "Please enable $provider",
            Toast.LENGTH_LONG
        ).show()

        disableGps(false)
    }

    override fun onLocationChanged(p0: Location) {
        Log.d("GEOLOCATION", "new latitude: ${p0.latitude} and longitude: ${p0.longitude}")

        if (path.actualPoints.isEmpty()) {
            onFirstLocation()
        }

        val point = GeoPoint(p0.latitude, p0.longitude)
        map.controller.setCenter(point)

        // Marker
        marker.position = point
        marker.title =
            point.longitude.roundToDecimal(5).toString() + ',' + point.latitude.roundToDecimal(5)
                .toString()

        CoroutineScope(Dispatchers.IO).launch {
            marker.subDescription = getAddress(point)
        }
        marker.alpha = 1f

        // Path
        path.addPoint(point)

        map.invalidate()
    }

    private fun onFirstLocation()
    {
        map.controller.setZoom(18.0)
        info.startAnimation(appear)
    }

    private fun getAddress(point: GeoPoint): String {
        return try {
            val geocoder = Geocoder(this.context)
            val list = geocoder.getFromLocation(point.latitude, point.longitude, 1)
            list[0].getAddressLine(0)
        } catch (e: IOException) {
            "" // Return empty if at a location without an address.
        }
    }

}