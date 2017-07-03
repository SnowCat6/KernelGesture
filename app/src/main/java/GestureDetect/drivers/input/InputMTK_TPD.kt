package GestureDetect.drivers.input

import GestureDetect.GestureDetect

/*
MT touchscreen with gestures
 */
open class InputMTK_TPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean{
        return name.toLowerCase() == "mtk-tpd"
    }
    override fun onEvent(ev:List<String>):String?
            = filter(ev[1])
}