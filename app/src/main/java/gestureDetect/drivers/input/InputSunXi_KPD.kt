package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput

/*
SunXI tablet
 */
open class InputSunXi_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean
    {
        if (!arrayOf("sun4i-keyboard")
                .contains(name.toLowerCase())) return false

        return onDetectKeys(name)
    }

    override fun onEvent(ev: SensorInput.EvData): String?
    {
        val keys = arrayOf(
                Pair("KEY_MENU",     "KEY_VOLUMEUP"),
                Pair("KEY_SEARCH",   "KEY_VOLUMEDOWN"))

        return filter(ev, ev.evButton, keys)
    }
}