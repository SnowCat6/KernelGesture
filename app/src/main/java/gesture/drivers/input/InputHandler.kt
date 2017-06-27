package gesture.drivers.input

import gesture.GestureDetect

abstract class InputHandler(val gesture:GestureDetect)
{
    abstract fun onDetect(name:String):Boolean
    abstract fun onEvent(line:String):String?
    open fun setEnable(enable:Boolean) {}
    open fun getEnable():Boolean = false

    companion object
    {
        //  Allowed standard key
        val allowGestures = arrayOf(
                "KEY_UP",
                "KEY_DOWN",
                "KEY_LEFT",
                "KEY_RIGHT",
                "KEY_U",
                "KEY_C",
                "KEY_O",
                "KEY_W",
                "KEY_E",
                "KEY_V",
                "KEY_M",
                "KEY_Z",
                "KEY_S",
                "KEY_VOLUMEUP",
                "KEY_VOLUMEDOWN",
                "KEY_PROXIMITY"
        )

        fun filter(key: String?, convert: Array<Pair<String, String>>? = null): String?
        {
            if (key == null || key.isEmpty())
                return null

            var gesture = key
            if (convert != null) {
                for ((first, second) in convert) {
                    if (key != first) continue
                    gesture = second
                    break
                }
            }
            return if (gesture in allowGestures) gesture else null
        }
    }
}