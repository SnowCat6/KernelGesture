package gesture.drivers.sensor

import gesture.GestureDetect

abstract class SensorHandler(val gesture:GestureDetect)
{
    abstract fun onDetect():Boolean
    open fun onStart() {}
    open fun onStop() {}
    open fun close(){}

    open fun enable(bEnable:Boolean) {}

    fun sensorEvent(event:String):Boolean{
        return gesture.sensorEvent(event)
    }
}