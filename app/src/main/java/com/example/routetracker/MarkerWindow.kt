package com.example.routetracker

import android.content.Context
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContentProviderCompat.requireContext
import org.osmdroid.bonuspack.location.NominatimPOIProvider
import org.osmdroid.bonuspack.location.POI
import org.osmdroid.bonuspack.routing.OSRMRoadManager
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
    // var roadManager: RoadManager = OSRMRoadManager(this., MY_USER_AGENT)
   // private var pois: MutableList<POI> = mutableListOf()
    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)
        // val path = Polyline(mapView, true)
        val routeButton = mView.findViewById<Button>(R.id.btRoute)
     /*   val userAgent = BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME
        val poiProvider = NominatimPOIProvider(userAgent)
        pois = poiProvider.getPOIInside(mapView.boundingBox, "Restaurant", 30)

      */
        val titleText = mView.findViewById<TextView>(R.id.tvTitle)

        routeButton.setOnClickListener {


            //  mapView.addMapListener(MoveMarkerMapListener)
        }
        mView.setOnClickListener {
            close()
        }
    }

    override fun onClose() {
        close()
    }
}