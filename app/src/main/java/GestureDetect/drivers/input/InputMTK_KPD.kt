package GestureDetect.drivers.input

import SuperSU.ShellSU
import GestureDetect.GestureDetect
import GestureDetect.GestureAction

/*
MTK and QCOMM keyboard
 */
open class InputMTK_KPD(gesture: GestureDetect) : InputHandler(gesture)
{
    private class GS(
            val detectFile: String,
            val setPowerON: String,
            val setPowerOFF: String,
            val getGesture: String
    )

    private var HCT_GESTURE_IO: GS? = null
    //  HCT version gesture for Android 5x and Android 6x
    private val HCT_GESTURE_PATH = arrayOf(
            //  Android 5.x HCT gestures
            GS("/sys/devices/platform/mtk-tpd/tpgesture_status",
                    "on > /sys/devices/platform/mtk-tpd/tpgesture_status",
                    "",
                    "/sys/devices/platform/mtk-tpd/tpgesture"),
            // Android 6.x HCT gestures
            GS("/sys/devices/bus/bus\\:touch@/tpgesture_status",
                    "on > /sys/devices/bus/bus\\:touch@/tpgesture_status",
                    "",
                    "/sys/devices/bus/bus\\:touch@/tpgesture"),
            //  Unknown 3.10 FTS touchscreen gestures for driver FT6206_X2605
            GS("/sys/class/syna/gesenable",
                    "1 > /sys/class/syna/gesenable",
                    "",
                    "")
    )
    val su = ShellSU()

    override fun setEnable(enable:Boolean)
    {
        if (HCT_GESTURE_IO == null) return
//  Change state when screen is off cause freeze!! Touchscreen driver BUG!!
        if (!GestureAction.HW.isScreenOn(context)) return

        val io = if (enable) HCT_GESTURE_IO!!.setPowerON else HCT_GESTURE_IO!!.setPowerOFF
        if (io.isNotEmpty()) su.exec("echo $io")
    }

    override fun onDetect(name:String): Boolean
    {
        if (name.toLowerCase() != "mtk-kpd") return false
        gesture.addSupport(arrayOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))

        if (HCT_GESTURE_IO == null) {
            for (it in HCT_GESTURE_PATH) {
                if (!su.isFileExists(it.detectFile)) continue
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
        if (ev[0] != "EV_KEY") return null

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

        if (HCT_GESTURE_IO == null) return null
        //  get gesture name
        val gs = su.getFileLine(HCT_GESTURE_IO!!.getGesture)
        return filter(gs, keys)
    }
}