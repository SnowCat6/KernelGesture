package gesture

interface InputHandler
{
    fun onDetect(name:String):Boolean
    fun onEvent(line:String):String?
    fun setEnable(enable:Boolean){}
    fun getEnable():Boolean{ return false }
}