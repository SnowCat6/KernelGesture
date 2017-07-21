package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput

/*
MTK and QCOMM keyboard
 */
open class InputMTK_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    //  HCT version gesture for Android 5x and Android 6x
    override val GESTURE_PATH = arrayOf(
            //  Android 5.x HCT gestures
            GS("/sys/devices/platform/mtk-tpd/tpgesture_status",
                    "on","off",
                    "/sys/devices/platform/mtk-tpd/tpgesture"),
            // Android 6.x HCT gestures
            GS("/sys/devices/bus/bus\\:touch@/tpgesture_status",
                    "on", "off",
                    "/sys/devices/bus/bus\\:touch@/tpgesture"),
                    // Android 7.x Oukitel K6000 Plus
            GS("/sys/devices/soc/soc:touch/tpgesture_status",
            "on", "off",
            "/sys/devices/soc/soc:touch/tpgesture")
    )

    override fun onDetect(name:String): Boolean
    {
        if (!onDetect(name, arrayOf("mtk-kpd")))
            return false

        super.onDetect(name)
        onDetectKeys(name)
        return true
    }

    override fun onEvent(ev: SensorInput.EvData):String?
    {
        when(ev.evButton){
            "KEY_PROG3" ->  return onEventHCT(ev)
            "KEY_VOLUMEUP",
            "KEY_VOLUMEDOWN" ->  return filter(ev, ev.evButton)
        }

        //  Many others device conversion
        //  Oukitel K4000 device gesture
        val keys = arrayOf(
                Pair("BTN_PINKIE",  "KEY_UP"),
                Pair("BTN_BASE",    "KEY_DOWN"),
                Pair("BTN_BASE2",   "KEY_LEFT"),
                Pair("BTN_BASE3",   "KEY_RIGHT"),
                Pair("BTN_JOYSTICK","KEY_U"),
                Pair("BTN_THUMB",   "KEY_C"),
                Pair("BTN_THUMB2",  "KEY_E"),
                Pair("BTN_TOP2",    "KEY_O"),
                Pair("012c",        "KEY_S"),
                Pair("BTN_BASE6",   "KEY_V"),
                Pair("BTN_BASE4",   "KEY_W"),
                Pair("BTN_TOP",     "KEY_M"),
                Pair("BTN_BASE5",   "KEY_Z"),
                Pair("KEY_WAKEUP",  "KEY_U")
        )
        return filter(ev, ev.evButton, keys)
    }
    //  HCT gesture give from file
    private fun onEventHCT(ev: SensorInput.EvData):String?
    {
        val keys = arrayOf(
                Pair("UP",          "KEY_UP"),
                Pair("DOWN",        "KEY_DOWN"),
                Pair("LEFT",        "KEY_LEFT"),
                Pair("RIGHT",       "KEY_RIGHT"),
                Pair("DOUBCLICK",   "KEY_U"),
                Pair("c",           "KEY_C"),
                Pair("o",           "KEY_O"),
                Pair("w",           "KEY_W"),
                Pair("e",           "KEY_E"),
                Pair("v",           "KEY_V"),
                Pair("m",           "KEY_M"),
                Pair("z",           "KEY_Z"),
                Pair("s",           "KEY_S")
        )

        GESTURE_IO?.apply {
            return filter(ev, getGesture(), keys)
        }
        return null
    }
}