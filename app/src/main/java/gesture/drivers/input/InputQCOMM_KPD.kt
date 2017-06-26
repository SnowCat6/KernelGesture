package gesture.drivers.input

import gesture.GestureDetect

/*
Qualcomm keys
 */
open class InputQCOMM_KPD : InputHandler
{
    override fun onDetect(gesture:GestureDetect, name:String):Boolean {
        if (!arrayOf("qpnp_pon",  "gpio-keys")
                .contains(name.toLowerCase())) return false

        gesture.addSupport(arrayOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))
        return true
    }

    override fun onEvent(gesture:GestureDetect, line: String): String? {
        val arg = line.replace(Regex("\\s+"), " ").split(" ")
        if (arg[0] != "EV_KEY") return null
        return GestureDetect.GS.runGesture(arg[1])
    }
}