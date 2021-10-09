package com.example.routetracker

import android.widget.Button
import android.widget.TextView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.coroutines.coroutineContext

class MarkerWindow(mapView: MapView) :
    InfoWindow(R.layout.info_window, mapView) {
    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)

        mView.setOnClickListener {
            close()
        }
    }

  fun clkFunc(f:() -> Unit, s: (h: GeoPoint) -> Unit){
    val routeButton = view.findViewById<Button>(R.id.btRoute)
    routeButton.setOnClickListener {
        //TODO route suggestion


    }
}
//setting the title of the place to the textview
    fun seTitle(title: String){
        view.findViewById<TextView>(R.id.tvTitle).text = title
    }

    override fun onClose() {
        close()
    }

}