package gestureDetect.drivers.input

import gestureDetect.GestureDetect

/*
Qualcomm keys
 */
open class InputQCOMM_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean {
        if (!arrayOf("qpnp_pon",  "gpio-keys", "hbtp_vm")
                .contains(name.toLowerCase())) return false

        super.onDetect(name)
        return onDetectKeys(name)
    }
}