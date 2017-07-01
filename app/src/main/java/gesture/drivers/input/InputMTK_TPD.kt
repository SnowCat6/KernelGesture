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
    override fun onEvent(ev:List<String>):String?
    {
        if (ev[0] != "EV_KEY") return null
        return filter(ev[1])
    }
}