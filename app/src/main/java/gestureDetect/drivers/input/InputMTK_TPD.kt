package gestureDetect.drivers.input

import gestureDetect.GestureDetect
import gestureDetect.drivers.sensor.SensorInput

/*
MT touchscreen with gestures
 */
open class InputMTK_TPD(gesture: GestureDetect) : InputHandler(gesture)
{
    override fun onDetect(name:String):Boolean
    {
        if (name.toLowerCase() != "mtk-tpd") return false

        return super.onDetectTouch(name)
    }
}