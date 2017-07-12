package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput

/*
MTK and QCOMM keyboard
 */
open class InputMTK_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    private var HCT_GESTURE_IO: GS? = null
    //  HCT version gesture for Android 5x and Android 6x
    private val HCT_GESTURE_PATH = arrayOf(
            //  Android 5.x HCT gestures
            GS("/sys/devices/platform/mtk-tpd/tpgesture_status",
                    "on","off",
                    "/sys/devices/platform/mtk-tpd/tpgesture"),
            // Android 6.x HCT gestures
            GS("/sys/devices/bus/bus\\:touch@/tpgesture_status",
                    "on", "off",
                    "/sys/devices/bus/bus\\:touch@/tpgesture"),
            //  Unknown 3.10 FTS touchscreen gestures for driver FT6206_X2605
            GS("/sys/class/syna/gesenable",
                    "1","0"),
            //  Doogee x5 Max Pro
            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-0038/gesture",
                    "1", "0"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-0038/gesture",
                    "1", "0"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-005d/gesture",
            "1", "0"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-005d/gesture",
                    "1", "0"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-0020/gesture",
                    "1", "0"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-0020/gesture",
                    "1", "0"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-004b/gesture",
                    "1", "0"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-004b/gesture",
                    "1", "0")
    )

    override fun setEnable(enable:Boolean)
            = setEnable(enable, HCT_GESTURE_IO)

    override fun onDetect(name:String): Boolean
    {
        if (!arrayOf("mtk-kpd")
                .contains(name.toLowerCase())) return false

        if (HCT_GESTURE_IO == null) HCT_GESTURE_IO = onDetectGS(HCT_GESTURE_PATH)
        return onDetectKeys(name)
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
                Pair("BTN_BASE5",   "KEY_Z")
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

        HCT_GESTURE_IO?.apply {
            return filter(ev, gesture.su.getFileLine(this.getGesture), keys)
        }
        return null
    }
}