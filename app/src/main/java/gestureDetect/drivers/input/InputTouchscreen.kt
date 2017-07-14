package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput

/*
MT touchscreen with gestures
 */
open class InputTouchscreen(gesture: GestureDetect) : InputHandler(gesture)
{
    //  HCT version gesture for Android 5x and Android 6x
    override val GESTURE_PATH = arrayOf(
            GS("/sys/class/gesture/gesture_ft5x06/enable",
                    "1", "0"),
            //  Xiaomi?
            GS("/data/tp/wakeup_mode",
                    "1", "0")
    )
    override fun onDetect(name:String):Boolean
    {
        if (!arrayOf("mtk-tpd", "ft5x06_ts", "atmel-maxtouch", "fts_ts")
                .contains(name.toLowerCase()))
            return false

        super.onDetect(name)
        return super.onDetectTouch(name)
    }
    override fun onEvent(ev: SensorInput.EvData): String?
    {
        val keys = arrayOf(
                Pair("KEY_WAKEUP",  "KEY_U")
        )
        val gesture = filter(ev, ev.evButton, keys)
        if (gesture != null && gesture.isNotEmpty())
            return gesture

        return super.onEvent(ev)
    }
}