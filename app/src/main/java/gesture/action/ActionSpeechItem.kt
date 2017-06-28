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

        try {
            tts?.language = Locale.getDefault()
            tts?.speak(value, TextToSpeech.QUEUE_FLUSH, null, "")

            if (!bNotify) Thread.sleep(500)
        }catch (e:Exception){
            e.printStackTrace()
        }

        return false
    }

    override fun onStart()
    {
        if (tts != null) return
        tts = TextToSpeech(action.context, this)
    }

    override fun close()
    {
        try {
            tts?.shutdown()
        }catch (e:Exception){
            e.printStackTrace()
        }
        tts = null
    }

    override fun onInit(status: Int)
    {
        if (status != TextToSpeech.SUCCESS)
        {
            try {
                tts?.shutdown()
            }catch (e:Exception){
                e.printStackTrace()
            }
            tts = null
            return
        }

        try {
            tts?.language = Locale.getDefault()
            tts?.speak("", TextToSpeech.QUEUE_FLUSH, null, "")
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}