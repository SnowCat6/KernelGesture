package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput

/*
MT touchscreen with gestures
 */
open class InputTouchscreen(gesture: GestureDetect) : InputHandler(gesture)
{
    private var GESTURE_IO: GS? = null
    //  HCT version gesture for Android 5x and Android 6x
    private val GESTURE_PATH = arrayOf(
            GS("/sys/class/gesture/gesture_ft5x06/enable", "1", "0"),
            //  Xiaomi?
            GS("/data/tp/wakeup_mode", "1", "0")
    )
    override fun onDetect(name:String):Boolean
    {
        if (!arrayOf("mtk-tpd", "ft5x06_ts", "atmel-maxtouch")
                .contains(name.toLowerCase()))
            return false

        if (GESTURE_IO == null) GESTURE_IO = onDetectGS(GESTURE_PATH)
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

    override fun setEnable(enable: Boolean)
            = setEnable(enable, GESTURE_IO)
}