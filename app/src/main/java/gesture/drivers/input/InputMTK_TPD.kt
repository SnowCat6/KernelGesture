package gesture.drivers.input

import gesture.GestureDetect

/*
MT touchscreen with gestures
 */
open class InputMTK_TPD : InputHandler
{
    override fun onDetect(gesture:GestureDetect, name:String):Boolean{
        return name.toLowerCase() == "mtk-tpd"
    }
    override fun onEvent(gesture:GestureDetect, line:String):String?
    {
        val arg = line.replace(Regex("\\s+"), " ").split(" ")
        if (arg[0] == "EV_KEY") return GestureDetect.GS.runGesture(arg[1])
        return null
    }
}