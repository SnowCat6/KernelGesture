package gestureDetect.drivers.input

import android.util.Log
import gestureDetect.GestureDetect
import gestureDetect.drivers.SensorInput
import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import ru.vpro.kernelgesture.BuildConfig


/**
 * Базовый класс для получения события для конкретных устройств
 */
abstract class InputHandler(val gesture:GestureDetect)
{
    val context = gesture.context
    var rawFilter = "EV_KEY"
    val size = Point()
    var coordinates = Point()

    var GESTURE_IO: GS? = null
    open val GESTURE_PATH = arrayOf<GS>()

    inner class GS(
            val powerFile: String,
            val setPowerON: String = "1",
            val setPowerOFF: String = "0",
            val getGesture: String = ""
    ){
        fun setEnable(bEnable:Boolean)
        {
            val io = if (bEnable) setPowerON else setPowerOFF
            if (io.isNotEmpty()) gesture.su.exec("echo $io > $powerFile")
        }
        fun getGesture():String?
                = if (getGesture.isNotEmpty()) gesture.su.getFileLine(getGesture) else null
    }
    /**
     * Определить возможность получения событий по имени /dev/input устройства
     */
    open fun onDetect(name:String):Boolean
    {
        for (it in GESTURE_PATH)
        {
            if (!gesture.su.isFileExists(it.powerFile)) continue
            gesture.addSupport("GESTURE_HW")
            gesture.addSupport(allowGestures)
            GESTURE_IO = it
            break
        }

        return false
    }
    fun onDetect(name:String, names:Array<String>):Boolean
            = names.contains(name.toLowerCase())

    fun onDetect(name:String, regs:Array<Regex>):Boolean{
        val n = name.toLowerCase()
        return regs.firstOrNull { n.contains(it) } != null
    }
    fun onDetect(name:String, names:Array<String>, regs:Array<Regex>):Boolean
            = onDetect(name, names) || onDetect(name, regs)

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
    var lastTouchTime = 0.0
    open fun onEvent(ev: SensorInput.EvData): String?
    {
        if (ev.evButton != "BTN_TOUCH")
            return filter(ev, ev.evButton)

        val timeout = ev.evMilliTime - lastTouchTime

        val dx = (coordinates.x-ev.coordinates.x).toDouble()
        val dy = (coordinates.y-ev.coordinates.y).toDouble()
        val radius = Math.sqrt(dx*dx + dy*dy).toInt()

        coordinates = ev.coordinates
        lastTouchTime = ev.evMilliTime

        if (ev.coordinates.y !in 0 .. size.y ||
                ev.coordinates.x !in 0 .. size.x) return null

        val maxR = Math.min(size.x, size.y)/16
        if (BuildConfig.DEBUG) {
            Log.d("Double tap", "r:$radius, rMax:$maxR, t:$timeout")
        }
        if (timeout !in 0.025 .. 0.500 || radius > maxR)
            return null

        if (!gesture.settings.getEnable("KEY_U_ON"))
            return null

        if (!gesture.hw.isHomeScreen())
            return null

        return "KEY_U_ON"
    }
    /**
     * Включить или выключить распознование жестов
     */
    open fun setEnable(enable:Boolean) {
        //  Change state when screen is off cause sensor freeze!! Touchscreen driver BUG!!
        if (!gesture.hw.isScreenOn()) return
        GESTURE_IO?.setEnable(enable)
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

        val gesture = convert?.firstOrNull { it.first == key }?.second ?: key
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