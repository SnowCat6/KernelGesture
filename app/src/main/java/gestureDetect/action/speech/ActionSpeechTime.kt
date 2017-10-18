package gestureDetect.action.speech

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.BuildConfig
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawableEx
import java.text.MessageFormat
import java.util.*

/**
 * Time speech action
 */
class ActionSpeechTime(action: GestureAction) : ActionSpeechItem(action)
{
    override fun action(context: Context): String?
            = if (isSpeechSupport()) "speech.time" else ""

    override fun name(context: Context): String?
            = context.getString(R.string.ui_action_speech_time)

    override fun icon(context: Context): Drawable?
            = context.getDrawableEx(R.drawable.icon_speech_time)

    override fun run(context: Context): Boolean {
        val result = MessageFormat.format("{0,time,short}", Date())
        if (BuildConfig.DEBUG) {
            Log.d("Time is", result)
        }
        return doSpeech(result)
    }
}