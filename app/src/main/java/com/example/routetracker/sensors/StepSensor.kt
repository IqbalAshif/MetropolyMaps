package com.example.routetracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.TextView
import com.example.routetracker.R

class StepSensor(context : Context?) : SensorEventListener
{
    private val sm: SensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    var onSensorTriggered : (SensorEvent) -> Unit = {}
    var onSensorStarted : () -> Unit = {}
    var onSensorStopped : () -> Unit = {}

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0 != null) {
            onSensorTriggered(p0)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    fun enable() {
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        onSensorStarted()
    }

    fun disable() {
        sm.unregisterListener(this)
        onSensorStopped()
    }

}