package com.example.routetracker.helpers

import android.content.Context
import android.location.Geocoder
import org.osmdroid.util.GeoPoint
import java.io.IOException
import java.lang.Error

fun getAddress(context : Context?, point: GeoPoint): String {
    return try {
        val geocoder = Geocoder(context)
        val list = geocoder.getFromLocation(point.latitude, point.longitude, 1)
        list[0].getAddressLine(0)
    } catch (e: IOException) {
        "" // Return empty if at a location without an address.
    }
}

fun parseLocation(address : String) : GeoPoint?
{

    val arg : List<String> = if(address.startsWith("geo:",ignoreCase = true))
        address.drop("geo:".count()).split(',','?') // [Long, lat, parameters]
    else
        address.split(',','?') // [Long, lat, parameters]

    return try {
        GeoPoint(arg[0].toDouble(),arg[1].toDouble())
    } catch (err : Error) {
        null
    }
}