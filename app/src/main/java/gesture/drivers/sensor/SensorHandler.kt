package gesture.drivers.sensor

import android.content.Context
import gesture.GestureDetect

interface SensorHandler {
    fun onDetect():Boolean
    fun onStart()
    fun onStop()

    fun sensorEvent(event:String):Boolean{
        return GestureDetect.sensorEvent("KEY_PROXIMITY")
    }
}