package gestureDetect.drivers.input

import gestureDetect.GestureDetect

/*
Qualcomm keys
 */
open class InputQCOMM_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean {
        // , "hbtp_vm" - virtual mouse?
        if (!arrayOf("qpnp_pon",  "gpio-keys")
                .contains(name.toLowerCase())) return false

        super.onDetect(name)
        return onDetectKeys(name)
    }
}