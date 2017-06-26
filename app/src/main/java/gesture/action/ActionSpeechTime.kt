package gesture

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import ru.vpro.kernelgesture.R
import java.text.MessageFormat
import java.util.*

/**
 * Time speech action
 */
class ActionSpeechTime : ActionSpeechItem {
    override fun action(): String
            = "speech.time"

    override fun name(context: Context): String
            = context.getString(R.string.ui_speech_time)

    override fun icon(context: Context): Drawable
            = context.getDrawable(R.drawable.icon_speech_time)

    override fun run(context: Context): Boolean {
        val result = MessageFormat.format("{0,time,short}", Date())
        Log.d("Time is", result)
        return doSpeech(context, result)
    }
}