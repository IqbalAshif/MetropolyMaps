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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import androidx.fragment.app.replace
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration


class MapFragment : Fragment(), LocationListener, SensorEventListener {

    private lateinit var lm: LocationManager

    private lateinit var sm: SensorManager
    private var stepSensor: Sensor? = null
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
        sm = this.context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepCount = view.findViewById<TextView>(R.id.steps)

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
        toggle.setOnClickListener{toggleGps()}

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
        disableStepSensor()
    }

    private fun toggleGps()
    {
        if (toggle.tag == false && requestPermissions())
            enableGps() // Start recording
        else
            disableGps() // Stop recording
    }

    @SuppressLint("MissingPermission")
    private fun enableGps() {
        if (locationProvider() != null) {
            enableStepSensor()
            toggle.tag = true
            toggle.backgroundTintList =
                ColorStateList.valueOf(Color.RED + Color.RED * 40 / 100)
            lm.requestLocationUpdates(locationProvider()!!, 1000, 15f, this)
        } else {
            Log.e("Location", "Insufficient permissions, location needed")
            Toast.makeText(
                this.context?.applicationContext,
                "Insufficient permissions, location needed",
                Toast.LENGTH_LONG
            ).show()

            requestPermissions()
        }
    }

    private fun onFirstLocation()
    {
        map.controller.setZoom(18.0)
        info.startAnimation(appear)
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
        disableStepSensor()
    }


    /* Permissions */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {}

    private fun hasLocationPermission(permission: String): Boolean =
        (ContextCompat.checkSelfPermission(
            this.requireContext(), permission
        ) == PackageManager.PERMISSION_GRANTED)


    private fun requestPermissions(): Boolean {
        val requiredPermissions: MutableList<String> = mutableListOf()
        var ret = true
        if ((ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            ret = false
        }
        if ((ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            ret = false
        }
        if ((ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            ret = false
        }
        if ((ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (requiredPermissions.isNotEmpty())
            ActivityCompat.requestPermissions(
                this.requireContext() as Activity, requiredPermissions.toTypedArray(),
                0
            )

        return ret

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
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        Toast.makeText(
            this.context,
            "Please enable $provider",
            Toast.LENGTH_LONG
        ).show()

        disableGps(false)
    }

    private fun locationProvider(): String? = when {
        hasLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION) -> {
            LocationManager.GPS_PROVIDER
        }
        hasLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
            LocationManager.NETWORK_PROVIDER
        }
        else -> {
            null
        }
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

    private fun getAddress(point: GeoPoint): String {
        return try {
            val geocoder = Geocoder(this.context)
            val list = geocoder.getFromLocation(point.latitude, point.longitude, 1)
            list[0].getAddressLine(0)
        } catch (e: IOException) {
            "" // Return empty if at a location without an address.
        }
    }

    /* Sensors */

    // Step
    override fun onSensorChanged(p0: SensorEvent?) {
        p0 ?: return
        if (p0.sensor == stepSensor) {
            Log.d("STEPCOUNT", "values: ${p0.values.toString()}")
            if (stepsTotal != null) {
                val valueToAdd = p0.values[0] - stepsTotal!!
                stepCount.tag = ((stepCount.tag as Int) + valueToAdd).toInt()
                "Steps: ${stepCount.tag}".also { stepCount.text = it }
            }

            stepsTotal = p0.values[0]
        }

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onPause() {
        super.onPause()
        if (stepSensor != null)
            sm.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (stepSensor != null)
            sm.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun enableStepSensor() {
        if (stepSensor != null) {
            stepCount.tag = 0
            stepCount.text = ""
            sm.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun disableStepSensor() {
        if (stepSensor != null) {
            sm.unregisterListener(this)
            stepsTotal = null
            stepCount.text = ""
        }
    }

    // Math
    private fun Double.roundToDecimal(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return Math.round(this * multiplier) / multiplier
    }

}
