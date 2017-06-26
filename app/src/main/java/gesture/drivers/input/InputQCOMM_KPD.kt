package gesture.drivers.input

import gesture.GestureDetect

/*
Qualcomm keys
 */
open class InputQCOMM_KPD : InputHandler
{
    override fun onDetect(name:String):Boolean {
        if (!arrayOf("qpnp_pon",  "gpio-keys")
                .contains(name.toLowerCase())) return false

        GestureDetect.SUPPORT.add(arrayOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))
        return true
    }

    override fun onEvent(line: String): String? {
        val arg = line.replace(Regex("\\s+"), " ").split(" ")
        if (arg[0] != "EV_KEY") return null
        return GestureDetect.GS.runGesture(arg[1])
    }
}