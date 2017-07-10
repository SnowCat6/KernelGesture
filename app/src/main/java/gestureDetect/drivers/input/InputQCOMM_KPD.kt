package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.sensor.SensorInput

/*
Qualcomm keys
 */
open class InputQCOMM_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean {
        super.onDetect(name)
        if (!arrayOf("qpnp_pon",  "gpio-keys")
                .contains(name.toLowerCase())) return false

        gesture.addSupport(listOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))
        return true
    }
}