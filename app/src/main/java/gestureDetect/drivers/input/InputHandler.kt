package gestureDetect.drivers.input

import android.util.Log
import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput
import android.content.Context
import android.graphics.Point
import android.view.WindowManager


/**
 * Базовый класс для получения события для конкретных устройств
 */
abstract class InputHandler(val gesture:GestureDetect)
{
    val context = gesture.context
    var rawFilter = "EV_KEY"
    val size = Point()

    data class GS(
            val detectPowerFile: String,
            val setPowerON: String,
            val setPowerOFF: String,
            val getGesture: String = ""
    )
    /**
     * Определить возможность получения событий по имени /dev/input устройства
     */
    abstract fun onDetect(name:String):Boolean

    fun onDetectGS(gs:Array<GS>):GS?
    {
        for (it in gs) {
            if (!gesture.su.isFileExists(it.detectPowerFile)) continue
            gesture.addSupport("GESTURE_HW")
            gesture.addSupport(allowGestures)
            return it
        }
        return null
    }

    open fun onDetectTouch(name:String):Boolean
    {
        rawFilter = "-e EV_KEY -e ABS_MT_POSITION"

        gesture.addSupport("GESTURE_ON")
        gesture.registerScreenEvents("KEY_U_ON", "KEY_U_ON")

        gesture.addSupport("GESTURE")
        gesture.addSupport(allowGestures)

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        display.getSize(size)

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
        if (ev.evButton != "BTN_TOUCH") {
            return filter(ev, ev.evButton)
        }

        if (!gesture.hw.isHomeScreen()) return null
        if (ev.y !in 0 .. size.y) return null

        val timeout = ev.evMilliTime - lastTouchTime
        lastTouchTime = ev.evMilliTime
        Log.d("Timeout", timeout.toString())
        if (timeout !in 0.0 .. 0.500) return null
        return "KEY_U_ON"
    }
    /**
     * Включить или выключить распознование жестов
     */
    open fun setEnable(enable:Boolean) {}

    fun setEnable(enable:Boolean, gs:GS?){
        //  Change state when screen is off cause sensor freeze!! Touchscreen driver BUG!!
        if (!gesture.hw.isScreenOn()) return

        gs?.apply {
            val io = if (enable) setPowerON else setPowerOFF
            if (io.isNotEmpty()) gesture.su.exec("echo $io > $detectPowerFile")
        }
    }
    /**
     * Получить текущее состояние
     */
    open fun getEnable():Boolean = false

    /**
     * Отфильтровать не поддерживаемые жесты или сконвертировать частные жесты в поддерживаемые
     */
    fun filter(ev: SensorInput.EvData, key: String?, convert: Array<Pair<String, String>>? = null): String?
    {
        if (key == null || key.isEmpty() || ev.evPress != "DOWN")
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