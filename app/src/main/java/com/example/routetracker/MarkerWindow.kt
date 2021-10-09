package com.example.routetracker

import android.content.Context
import android.database.Observable
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContentProviderCompat.requireContext
import org.osmdroid.bonuspack.location.NominatimPOIProvider
import org.osmdroid.bonuspack.location.POI
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.coroutines.coroutineContext

class MarkerWindow(mapView: MapView) :
    InfoWindow(R.layout.info_window, mapView) {
    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)

        val routeButton = mView.findViewById<Button>(R.id.btRoute)
        routeButton.setOnClickListener {
        //TODO route suggestion
        }
        mView.setOnClickListener {
            close()
        }
    }
//setting the title of the place to the textview
    fun seTitle(title: String){
        view.findViewById<TextView>(R.id.tvTitle).text = title
    }

   /* private fun retrievingRoad(roadManager: OSRMRoadManager, waypoints: ArrayList<GeoPoint>) {
        // Retrieving road

        val road = roadManager.getRoad(waypoints)
        val roadOverlay = RoadManager.buildRoadOverlay(road)
        mapView?.overlays?.add(roadOverlay);

        val nodeIcon = mapView?.context?.resources?.getDrawable(R.drawable.ic_baseline_location)
        for (i in 0 until road.mNodes.size) {
            val node = road.mNodes[i]
            val nodeMarker = Marker(mapView)
            nodeMarker.position = node.mLocation
            nodeMarker.setIcon(nodeIcon)
            nodeMarker.title = "Step $i"
            mapView?.overlays?.add(nodeMarker)
            nodeMarker.snippet = node.mInstructions;
            nodeMarker.subDescription = Road.getLengthDurationText(mapView?.context, node.mLength, node.mDuration);
        }
    }*/



    override fun onClose() {
        close()
    }
}