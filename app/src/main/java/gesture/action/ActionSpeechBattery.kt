package gesture

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import ru.vpro.kernelgesture.R

/**
 * Speech battery percent
 */
class ActionSpeechBattery : ActionSpeechItem
{
    override fun action(): String
            = "speech.battery"

    override fun name(context: Context): String
            = context.getString(R.string.ui_action_speech_battery)

    override fun icon(context: Context): Drawable
            = context.getDrawable(R.drawable.icon_speech_battery)

    override fun run(context: Context): Boolean
    {
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val prefix = context.getString(R.string.ui_action_battery)

        return doSpeech(context, "$prefix $batLevel%")
    }
}