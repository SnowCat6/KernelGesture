package gesture.drivers.input

import gesture.GestureDetect

//  Unknown FT5x06_ts gesture solution
open class InputFT5x06_ts : InputHandler
{
    override fun onDetect(name: String): Boolean {
        if (name.toLowerCase() != "ft5x06_ts") return false
        return true
    }

    override fun onEvent(line: String): String? {
        val arg = line.replace(Regex("\\s+"), " ").split(" ")
        if (arg[0] != "EV_KEY") return null
        return GestureDetect.GS.runGesture(arg[1])
    }

    override fun setEnable(enable: Boolean)
    {
        GestureDetect.SU.exec("echo ${if (enable) 1 else 0} > sys/class/gesture/gesture_ft5x06/enable")
    }
}