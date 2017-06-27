package gesture.drivers.sensor

import gesture.GestureDetect

open class SensorHandler(val gesture:GestureDetect)
{
    open fun onDetect():Boolean = false
    open fun onStart() = Unit
    open fun onStop() = Unit

    fun sensorEvent(event:String):Boolean{
        return gesture.sensorEvent(event)
    }
}