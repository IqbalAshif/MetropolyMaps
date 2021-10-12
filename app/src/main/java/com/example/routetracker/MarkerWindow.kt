package com.example.routetracker

import android.annotation.SuppressLint
import android.content.Context
import android.text.Layout
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import com.example.routetracker.helpers.locationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.coroutines.coroutineContext

class MarkerWindow(val context: Context, mapView: MapView, val mapFragment: MapFragment) :
    InfoWindow(R.layout.info_window, mapView) {
    lateinit var onRoute: () -> Unit

    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)
        val routeButton = view.findViewById<Button>(R.id.btRoute)
        //clicking route button
        routeButton.setOnClickListener {
            onRoute()

        }

        mView.setOnClickListener {
            close()
        }
    }

    //setting the title of the place to the textview
    fun seTitle(title: String) {
        view.findViewById<TextView>(R.id.tvTitle).text = title
    }


    private fun addMarker(point: GeoPoint, title: String) {
        val startMarker = Marker(mapView)
        startMarker.position = point
        startMarker.title = title
        startMarker.icon = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_position)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        if (mapFragment.destinationMarker != null) {
            mapView.overlays.remove(mapFragment.destinationMarker)
        }
        mapFragment.destinationMarker = startMarker
        mapView.overlays?.add(startMarker)
        mapView.invalidate()

    }

    fun addingRouteLocations(startPoint: GeoPoint, endPoint: GeoPoint) {
        val roadManager = OSRMRoadManager(context, "MY_USER_AGENT")

        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(startPoint)
        routePoints.add(endPoint)
        CoroutineScope(Dispatchers.IO).launch {
            gettingRoad(roadManager, routePoints)
            mapView.invalidate()
        }
        addMarker(endPoint, "Destination")
    }

    private fun gettingRoad(roadManager: OSRMRoadManager, waypoints: ArrayList<GeoPoint>) {
        // Retrieving road
        roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val road = roadManager.getRoad(waypoints)
        if (mapFragment.route != null) {
            mapView.overlays.remove(mapFragment.route)
        }
        val roadOverlay = RoadManager.buildRoadOverlay(road, 0xAA0000FF.toInt(), 10.5F)
        mapFragment.route = roadOverlay
        mapView.overlays.add(roadOverlay)
        //Marker at each node
        val nodeIcon = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_stop_24)
        for (i in 0 until road.mNodes.size) {
            val node = road.mNodes[i]
            val nodeMarker = Marker(mapView)
            nodeMarker.position = node.mLocation
            nodeMarker.icon = nodeIcon
            nodeMarker.title = "Step $i"
            mapView.overlays.add(nodeMarker)
            nodeMarker.snippet = node.mInstructions
            nodeMarker.subDescription =
                Road.getLengthDurationText(mapView.context, node.mLength, node.mDuration)
        }

        mapView.invalidate()
    }

    override fun onClose() {
        close()
    }

}