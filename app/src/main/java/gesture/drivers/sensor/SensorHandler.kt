package gesture

import android.content.Context

interface SensorHandler {
    fun onDetect():Boolean
    fun onStart()
    fun onStop()
}