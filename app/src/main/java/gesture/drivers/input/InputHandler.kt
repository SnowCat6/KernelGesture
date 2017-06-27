package gesture.drivers.input

import gesture.GestureDetect

open class InputHandler(val gesture:GestureDetect)
{
    open fun onDetect(name:String):Boolean = false
    open fun onEvent(line:String):String? = null
    open fun setEnable(enable:Boolean) = Unit
    open fun getEnable():Boolean = false

    companion object
    {
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
            return if (gesture in allowGestures) gesture else null
        }
    }
}