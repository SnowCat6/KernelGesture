package gestureDetect.drivers.input

import gestureDetect.GestureDetect

/*
MTK and QCOMM keyboard
 */
open class InputMTK_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    private class GS(
            val detectPowerFile: String,
            val setPowerON: String,
            val setPowerOFF: String,
            val getGesture: String = ""
    )

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
                    "1","0")
    )

    override fun setEnable(enable:Boolean)
    {
        //  Change state when screen is off cause sensor freeze!! Touchscreen driver BUG!!
        if (!gesture.hw.isScreenOn()) return

        HCT_GESTURE_IO?.apply {
            val io = if (enable) setPowerON else setPowerOFF
            if (io.isNotEmpty()) gesture.su.exec("echo $io > $detectPowerFile")
        }
    }

    override fun onDetect(name:String): Boolean
    {
        if (name.toLowerCase() != "mtk-kpd") return false
        gesture.addSupport(listOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))

        if (HCT_GESTURE_IO == null) {
            for (it in HCT_GESTURE_PATH) {
                if (!gesture.su.isFileExists(it.detectPowerFile)) continue
                HCT_GESTURE_IO = it
                gesture.addSupport("GESTURE")
                gesture.addSupport(allowGestures)
                break
            }
        }
        return true
    }

    override fun onEvent(ev:List<String>):String?
    {
        when(ev[1]){
            "KEY_PROG3" ->  return onEventHCT()
            "KEY_VOLUMEUP",
            "KEY_VOLUMEDOWN" ->  return filter(ev[1])
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
        return filter(ev[1], keys)
    }
    //  HCT gesture give from file
    private fun onEventHCT():String?
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
            return filter(gesture.su.getFileLine(this.getGesture), keys)
        }
        return null
    }
}