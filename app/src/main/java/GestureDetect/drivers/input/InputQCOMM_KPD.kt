package GestureDetect.drivers.input

import GestureDetect.GestureDetect

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

    override fun onEvent(ev: List<String>): String?
            = filter(ev[1])
}