package gestureDetect.action.speech

import android.content.Context
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
    override fun action(context: Context): String?
            = if (getBatteryLevel(context) is Int && isSpeechSupport()) "speech.battery" else ""

    override fun name(context: Context): String?
            = context.getString(R.string.ui_action_speech_battery)

    override fun icon(context: Context): Drawable?
            = context.getDrawableEx(R.drawable.icon_speech_battery)

    override fun run(context: Context): Boolean
    {
        val prefix = context.getString(R.string.ui_action_battery)
        return doSpeech("$prefix ${getBatteryLevel(context)}%")
    }
    private fun getBatteryLevel(context : Context):Int?
    {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
                val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
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