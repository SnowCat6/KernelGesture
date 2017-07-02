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
            = context.getString(R.string.ui_action_speech_battery)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_speech_battery)

    override fun run(): Boolean
    {
        try {
            val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val prefix = context.getString(R.string.ui_action_battery)

            return doSpeech("$prefix $batLevel%")
        }catch (e:Exception){
            e.printStackTrace()
        }
        return false
    }
}