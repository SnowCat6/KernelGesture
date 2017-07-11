package gestureDetect.drivers.input

import android.util.Log
import gestureDetect.GestureDetect
import gestureDetect.drivers.sensor.SensorInput

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

    open fun onDetectTouch(name:String):Boolean
    {
        gesture.addSupport(listOf("GESTURE_ON"))
        gesture.registerScreenEvents("KEY_U_ON", "KEY_U_ON")

        return true
    }
    open fun onDetectKeys(name:String):Boolean
    {
        gesture.addSupport(listOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))

        gesture.registerDelayEvents("KEY_VOLUMEUP", "KEY_VOLUMEUP_DELAY")
        gesture.registerDelayEvents("KEY_VOLUMEDOWN", "KEY_VOLUMEDOWN_DELAY")

        gesture.registerScreenEvents("KEY_VOLUMEUP_DELAY", "KEY_VOLUMEUP_DELAY_ON")
        gesture.registerScreenEvents("KEY_VOLUMEDOWN_DELAY", "KEY_VOLUMEDOWN_DELAY_ON")

        return true
    }

    /**
     * Реакция на событие от устройства ввода
     */
    var lastTouchTime:Double = 0.0
    open fun onEvent(ev: SensorInput.EvData): String?
    {
        if (ev.evButton != "BTN_TOUCH")
            return filter(ev.evButton)

        val timeout = ev.evMilliTime - lastTouchTime
        lastTouchTime = ev.evMilliTime
        Log.d("Timeout", timeout.toString())
        if (timeout !in 0.0 .. 0.300) return null
        return filter("KEY_U_ON")
    }
    /**
     * Включить или выключить распознование жестов
     */
    open fun setEnable(enable:Boolean) {}

    /**
     * Получить текущее состояние
     */
    open fun getEnable():Boolean = false

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

    companion object
    {
        //  Allowed standard key
        val allowGestures = listOf(
                "KEY_UP",
                "KEY_DOWN",
                "KEY_LEFT",
                "KEY_RIGHT",
                "KEY_U",
                "KEY_U_ON", //  Double tap with screen is on
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
    }
}