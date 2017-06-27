package gesture.action

import android.content.Context.BATTERY_SERVICE
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import gesture.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Speech battery percent
 */
class ActionSpeechBattery(action: GestureAction) : ActionSpeechItem(action)
{
    override fun action(): String
            = "speech.battery"

    override fun name(): String
            = action.context.getString(R.string.ui_action_speech_battery)

    override fun icon(): Drawable
            = action.context.getDrawable(R.drawable.icon_speech_battery)

    override fun run(): Boolean
    {
        val bm = action.context.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val prefix = action.context.getString(R.string.ui_action_battery)

        return doSpeech("$prefix $batLevel%")
    }
}