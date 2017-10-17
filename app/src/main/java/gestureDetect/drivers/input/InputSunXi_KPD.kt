package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.tools.InputReader

/*
SunXI tablet
 */
open class InputSunXi_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean
    {
        if (!onDetect(name, arrayOf("sun4i-keyboard")))
            return false

        super.onDetect(name)
        onDetectKeys(name)
        return true
    }

    override fun onEvent(ev: InputReader.EvData): String?
    {
        val keys = arrayOf(
                Pair("KEY_MENU",     "KEY_VOLUMEUP"),
                Pair("KEY_SEARCH",   "KEY_VOLUMEDOWN"))

        return filter(ev, ev.evButton, keys)
    }
}