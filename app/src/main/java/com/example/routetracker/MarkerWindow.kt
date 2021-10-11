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

    lateinit var onClick : () -> Unit

    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)

        val routeButton = mView.findViewById<Button>(R.id.btRoute)
        routeButton.setOnClickListener {
            onClick()
        }

        mView.setOnClickListener {
            close()
        }
    }

    fun seTitle(title: String){
        view.findViewById<TextView>(R.id.tvTitle).text = title
    }

    override fun onClose() {
        close()
    }
}