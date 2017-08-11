package gestureDetect.drivers.input

import gestureDetect.GestureDetect

/*
ALE-L21
Huawei P8 Lite
 */
open class InputCypressTPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override val GESTURE_PATH = arrayOf(
            //  cyttsp5_mt
            GS("/sys/devices/amba.13/f7101000.i2c/i2c-1/1-001c/easy_wakeup_gesture",
                    "65535")
    )
    override fun onDetect(name:String):Boolean
    {
        if (!onDetect(name,
                arrayOf("cyttsp5_mt", Regex("hisi_gpio_key"))))
            return false

        super.onDetect(name)
        onDetectKeys(name)
        onDetectTouch(name)

        return true
    }
}