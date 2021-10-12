package com.example.routetracker.helpers

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.example.routetracker.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.location.NominatimPOIProvider
import org.osmdroid.bonuspack.location.POI
import org.osmdroid.util.BoundingBox
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

val userAgent = BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME

fun fetchPointsOfInterest(request: String, box: BoundingBox, result : (List<POI>) -> Unit)
{
    CoroutineScope(Dispatchers.IO).launch {
        val poiProvider = NominatimPOIProvider(userAgent)
        try {
            val poitemp = mutableListOf<POI>()
            request.split('|').forEach {
                poitemp.addAll(poiProvider.getPOIInside(box, it, 10))
            }
            Log.d("Points of Interest", poitemp.size.toString())
            result(poitemp)
        } catch (exp: NullPointerException) { // Network Error
            result(listOf())
            Log.e("Network Error", "Failed to fetch points of interest")
        }
    }
}

fun fetchPointsOfInterestUrl(query: String, result : (List<POI>) -> Unit)
{
    CoroutineScope(Dispatchers.IO).launch {
        val poiProvider = NominatimPOIProvider(userAgent)
        try {
            val poitemp = mutableListOf<POI>()
            poiProvider.getThem("https://nominatim.openstreetmap.org/search?format=json&q=${query.replace(" ","%20")}&limit=10")
            Log.d("Points of Interest", poitemp.size.toString())
            result(poitemp)
        } catch (exp: NullPointerException) { // Network Error
            result(listOf())
            Log.e("Network Error", "Failed to fetch points of interest")
        }
    }
}

