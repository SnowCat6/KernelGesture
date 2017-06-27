package gesture.drivers.input

import gesture.GestureDetect

/*
MT touchscreen with gestures
 */
open class InputMTK_TPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean{
        return name.toLowerCase() == "mtk-tpd"
    }
    override fun onEvent(line:String):String?
    {
        val arg = line.replace(Regex("\\s+"), " ").split(" ")
        if (arg[0] == "EV_KEY") return filter(arg[1])
        return null
    }
}