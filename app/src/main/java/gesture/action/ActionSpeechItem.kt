package gesture

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

/**
 * Common speech class
 */

interface ActionSpeechItem : ActionItem, TextToSpeech.OnInitListener {
    companion object {
        var tts: TextToSpeech? = null
    }

    fun doSpeech(context: Context, value: String): Boolean
    {
        GestureService.UI.vibrate(context)
        GestureService.UI.playNotifyToEnd(context)

        tts?.language = Locale.getDefault()
        tts?.speak(value, TextToSpeech.QUEUE_ADD, null)

        return false
    }

    override fun init(context: Context)
    {
        if (tts != null) return
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int)
    {
        if (status != TextToSpeech.SUCCESS) {
            tts = null
            return
        }

        tts?.language = Locale.getDefault()
        tts?.speak("", TextToSpeech.QUEUE_FLUSH, null)
    }
}