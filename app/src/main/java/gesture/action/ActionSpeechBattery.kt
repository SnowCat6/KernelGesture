package gesture.action

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import ru.vpro.kernelgesture.R

/**
 * Speech battery percent
 */
class ActionSpeechBattery(override val context: Context) : ActionSpeechItem
{
    override fun action(): String
            = "speech.battery"

    override fun name(): String
            = context.getString(R.string.ui_action_speech_battery)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_speech_battery)

    override fun run(): Boolean
    {
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val prefix = context.getString(R.string.ui_action_battery)

        return doSpeech("$prefix $batLevel%")
    }
}