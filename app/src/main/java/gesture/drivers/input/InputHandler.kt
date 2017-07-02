package gesture.drivers.input

import gesture.GestureDetect


/**
 * Базовый класс для получения события для конкретных устройств
 */
abstract class InputHandler(val gesture:GestureDetect)
{
    val context = gesture.context
    /**
     * Определить возможность получения событий по имени /dev/input устройства
     */
    abstract fun onDetect(name:String):Boolean

    /**
     * Реакция на событие от устройства ввода
     */
    abstract fun onEvent(ev:List<String>):String?

    /**
     * Включить или выключить распознование жестов
     */
    open fun setEnable(enable:Boolean) {}

    /**
     * Получить текущее состояние
     */
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

        /**
         * Отфильтровать не поддерживаемые жесты или сконвертировать частные жесты в поддерживаемые
         */
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