package gesture.drivers.input

import gesture.GestureDetect

interface InputHandler
{
    fun onDetect(gesture: GestureDetect, name:String):Boolean
    fun onEvent(gesture: GestureDetect, line:String):String?
    fun setEnable(enable:Boolean){}
    fun getEnable():Boolean{ return false }
}