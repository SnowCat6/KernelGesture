package gesture.drivers.input

import gesture.GestureDetect

/*
Qualcomm keys
 */
open class InputQCOMM_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean {
        if (!arrayOf("qpnp_pon",  "gpio-keys")
                .contains(name.toLowerCase())) return false

        gesture.addSupport(arrayOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))
        return true
    }

    override fun onEvent(ev: List<String>): String? {
        if (ev[0] != "EV_KEY") return null
        return filter(ev[1])
    }
}