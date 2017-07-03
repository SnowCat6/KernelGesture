package GestureDetect.drivers.input

import GestureDetect.GestureDetect

/*
SunXI tablet
 */
open class InputSunXi_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean {
        if (!arrayOf("sun4i-keyboard")
                .contains(name.toLowerCase())) return false

        gesture.addSupport(arrayOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))
        return true
    }

    override fun onEvent(ev: List<String>): String?
    {
        if (ev[0] != "EV_KEY") return null

        val keys = arrayOf(
                Pair("KEY_MENU",     "KEY_VOLUMEUP"),
                Pair("KEY_SEARCH",   "KEY_VOLUMEDOWN"))
        return filter(ev[1], keys)
    }
}