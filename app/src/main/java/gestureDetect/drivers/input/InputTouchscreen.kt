package gestureDetect.drivers.input

import gestureDetect.GestureDetect

/*
MT touchscreen with gestures
 */
open class InputTouchscreen(gesture: GestureDetect) : InputHandler(gesture)
{
    private var GESTURE_IO: GS? = null
    //  HCT version gesture for Android 5x and Android 6x
    private val GESTURE_PATH = arrayOf(
            GS("/sys/class/gesture/gesture_ft5x06/enable", "1", "0")
    )
    override fun onDetect(name:String):Boolean
    {
        if (!arrayOf("mtk-tpd", "ft5x06_ts")
                .contains(name.toLowerCase()))
            return false

        if (GESTURE_IO == null) {
            for (it in GESTURE_PATH) {
                if (!gesture.su.isFileExists(it.detectPowerFile)) continue
                GESTURE_IO = it
                gesture.addSupport("GESTURE")
                gesture.addSupport(allowGestures)
                break
            }
        }

        return super.onDetectTouch(name)
    }

    override fun setEnable(enable: Boolean)
    {
        //  Change state when screen is off cause sensor freeze!! Touchscreen driver BUG!!
        if (!gesture.hw.isScreenOn()) return

        GESTURE_IO?.apply {
            val io = if (enable) setPowerON else setPowerOFF
            if (io.isNotEmpty()) gesture.su.exec("echo $io > $detectPowerFile")
        }

    }
}