package gesture.action

import android.speech.tts.TextToSpeech
import gesture.GestureAction
import java.util.*

/**
 * Common speech class
 */

abstract class ActionSpeechItem(action: GestureAction) :
        ActionItem(action), TextToSpeech.OnInitListener
{
    var tts: TextToSpeech? = null

    fun doSpeech(value: String): Boolean
    {
        GestureAction.UI.vibrate(action.context)
        val bNotify = GestureAction.UI.playNotifyToEnd(action.context)

        tts?.language = Locale.getDefault()
        tts?.speak(value, TextToSpeech.QUEUE_FLUSH, null, "")

        if (!bNotify) Thread.sleep(500)

        return false
    }

    override fun onStart()
    {
        if (tts != null) return
        tts = TextToSpeech(action.context, this)
    }

    override fun onStop() {
        tts?.shutdown()
        tts = null
    }

    override fun onInit(status: Int)
    {
        if (status != TextToSpeech.SUCCESS) {
            tts = null
            return
        }

        tts?.language = Locale.getDefault()
        tts?.speak("", TextToSpeech.QUEUE_FLUSH, null, "")
    }
}