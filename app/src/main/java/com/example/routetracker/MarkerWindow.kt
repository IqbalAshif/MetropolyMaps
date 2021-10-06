package com.example.routetracker

import android.widget.Button
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

class MarkerWindow( mapView: MapView) :
    InfoWindow(R.layout.info_window, mapView) {
    
    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)

        val routeButton = mView.findViewById<Button>(R.id.btRoute)

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