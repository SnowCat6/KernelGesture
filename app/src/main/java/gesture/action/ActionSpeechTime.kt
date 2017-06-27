package gesture.action

import android.graphics.drawable.Drawable
import android.util.Log
import gesture.GestureAction
import ru.vpro.kernelgesture.R
import java.text.MessageFormat
import java.util.*

/**
 * Time speech action
 */
class ActionSpeechTime(action: GestureAction) : ActionSpeechItem(action)
{
    override fun action(): String
            = "speech.time"

    override fun name(): String
            = action.context.getString(R.string.ui_speech_time)

    override fun icon(): Drawable
            = action.context.getDrawable(R.drawable.icon_speech_time)

    override fun run(): Boolean {
        val result = MessageFormat.format("{0,time,short}", Date())
        Log.d("Time is", result)
        return doSpeech(result)
    }
}