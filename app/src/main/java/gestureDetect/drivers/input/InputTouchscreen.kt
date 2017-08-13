package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput

/*
MT touchscreen with gestures
 */
open class InputTouchscreen(gesture: GestureDetect) : InputHandler(gesture)
{
    //  HCT version gesture for Android 5x and Android 6x
    override var GESTURE_PATH = arrayOf(
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
                    "/sys/devices/soc/soc:touch/tpgesture"),
            //  Oukitel K6000 Pro
            GS("/sys/devices/bus/bus:touch@/tpgesture_status",
                    "on", "off",
                    "/sys/devices/bus/bus:touch@/tpgesture"),

            //  Unknown 3.10 FTS touchscreen gestures for driver FT6206_X2605
            GS("/sys/class/syna/gesenable"),

            //  Doogee x5 Max Pro
            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-0038/gesture"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-0038/gesture"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-005d/gesture"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-005d/gesture"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-0020/gesture"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-0020/gesture"),
            //  Jiayu S3 Advanced android 7.1.2
            GS("/sys/devices/bus.2/11007000.I2C0/i2c-0/0-0020/gesture"),

            GS("/sys/devices/platform/mt-i2c.0/i2c-0/0-004b/gesture"),
            GS("/sys/devices/bus/11008000.i2c/i2c-1/1-004b/gesture"),

            GS("/sys/class/gesture/gesture_ft5x06/enable"),
            //  Lenovo K920 (ROW)?
            GS("/sys/devices/f9924000.i2c/i2c-2/2-0038/gesture"),
            GS("/sys/devices/virtual/touchscreen/device/gesture"),
            //  T02 - [on/off]1 [I]1 [Z]0 [C]0 [V]0
            //  acer_z530?
            GS("/sys/devices/bus.3/11008000.I2C1/i2c-1/1-0038/gesturewordset",
                    "11111", "00000"),

            //  Xiaomi?
            GS("/data/tp/wakeup_mode")
    )
    override fun onDetect(name:String):Boolean
    {
        //  Touch
        if (onDetect(name,
                arrayOf("mtk-tpd", "atmel-maxtouch", "touch_dev",
                        Regex("ft.*_ts"), Regex("synaptics"))))
        {
            super.onDetect(name)
            super.onDetectTouch(name)
            return true
        }

        //  Keys
        if (onDetect(name,
                arrayOf("mtk-kpd")))
        {
            super.onDetect(name)
            super.onDetectKeys(name)
            return true
        }
        return false
    }
    override fun onEvent(ev: SensorInput.EvData): String?
    {
        when(ev.evButton){
            "KEY_PROG3" ->  return onEventHCT(ev)
            "KEY_VOLUMEUP",
            "KEY_VOLUMEDOWN" ->  return filter(ev, ev.evButton)
        }

        val keys = arrayOf(
                Pair("KEY_WAKEUP",          "KEY_U"),
                //  T02 gestures convert
                Pair("KEY_GESTURE_UP",      "KEY_UP"),
                Pair("KEY_GESTURE_DOWN",    "KEY_DOWN"),
                Pair("KEY_GESTURE_LEFT",    "KEY_LEFT"),
                Pair("KEY_GESTURE_RIGHT",   "KEY_RIGHT"),
                Pair("KEY_GESTURE_O",       "KEY_O"),
                Pair("KEY_GESTURE_E",       "KEY_E"),
                Pair("KEY_GESTURE_M",       "KEY_M"),
                Pair("KEY_GESTURE_L",       "KEY_L"),
                Pair("KEY_GESTURE_W",       "KEY_W"),
                Pair("KEY_GESTURE_S",       "KEY_S"),
                Pair("KEY_GESTURE_V",       "KEY_V"),
                Pair("KEY_GESTURE_C",       "KEY_C"),
                Pair("KEY_GESTURE_Z",       "KEY_Z"),
                //  Oukitel K4000 device gesture
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
        return super.onEvent(ev) ?: filter(ev, ev.evButton, keys)
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