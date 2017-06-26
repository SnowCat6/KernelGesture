package gesture.drivers.sensor

import gesture.GestureDetect

interface SensorHandler
{
    val gesture:GestureDetect

    fun onDetect():Boolean
    fun onStart()
    fun onStop()

    fun sensorEvent(event:String):Boolean{
        return gesture.sensorEvent(event)
    }
}