package gestureDetect.drivers.input

import SuperSU.ShellSU
import gestureDetect.GestureDetect
import gestureDetect.drivers.sensor.SensorInput

//  Unknown FT5x06_ts gesture solution
open class InputFT5x06_ts(gesture: GestureDetect) : InputHandler(gesture)
{
    val su = ShellSU()

    override fun onDetect(name: String): Boolean {
        if (name.toLowerCase() != "ft5x06_ts") return false
        super.onDetect(name)
        return true
    }

    override fun setEnable(enable: Boolean){
        su.exec("echo ${if (enable) 1 else 0} > sys/class/gesture/gesture_ft5x06/enable")
    }
}