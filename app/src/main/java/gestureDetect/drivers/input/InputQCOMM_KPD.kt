package gestureDetect.drivers.input

import gestureDetect.GestureDetect

/*
Qualcomm keys
 */
open class InputQCOMM_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean {
        if (!onDetect(name, arrayOf("qpnp_pon",  "gpio-keys")))
            return false

        super.onDetect(name)
        onDetectKeys(name)
        return true
    }
}