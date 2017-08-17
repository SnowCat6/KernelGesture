package gestureDetect.action.speech

import android.content.Context.BATTERY_SERVICE
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawableEx
import java.io.File

/**
 * Speech battery percent
 */
class ActionSpeechBattery(action: GestureAction) : ActionSpeechItem(action)
{
    override fun action(): String
            = if (getBatteryLevel() is Int && isSpeechSupport()) "speech.battery" else ""

    override fun name(): String
            = context.getString(R.string.ui_action_speech_battery)

    override fun icon(): Drawable
            = context.getDrawableEx(R.drawable.icon_speech_battery)

    override fun run(): Boolean
    {
        val prefix = context.getString(R.string.ui_action_battery)
        return doSpeech("$prefix ${getBatteryLevel()}%")
    }
    fun getBatteryLevel():Int?
    {
        var batLevel: Int = 0
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
                batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (batLevel > 0) return  batLevel
            }
        }catch (e:Exception) {}

        try {
            val f = File("/sys/class/power_supply/battery/capacity")
            return f.readText().trim().toInt()
        } catch (e:Exception) {}

        return null
    }
}