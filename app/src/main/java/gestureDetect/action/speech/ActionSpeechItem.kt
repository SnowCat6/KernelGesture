package gestureDetect.action.speech

import android.os.Build
import android.speech.tts.TextToSpeech
import com.google.firebase.crash.FirebaseCrash
import gestureDetect.GestureAction
import gestureDetect.action.ActionItem
import java.util.*

/**
 * Common speech class
 */

abstract class ActionSpeechItem(action: GestureAction) :
        ActionItem(action), TextToSpeech.OnInitListener
{
    var tts: TextToSpeech? = null

    fun isSpeechSupport():Boolean{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    fun doSpeech(value: String): Boolean
    {
        action.vibrate()

        try {
            tts?.apply {
                language = Locale.getDefault()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    speak(value, TextToSpeech.QUEUE_FLUSH, null, "")
                }
                Thread.sleep(1000)
            }
        }catch (e:Exception){
            e.printStackTrace()
            FirebaseCrash.report(e)
        }

        return false
    }

    override fun onStart()
    {
        if (tts != null) return
        tts = TextToSpeech(context, this)
    }

    override fun close()
    {
        try {
            tts?.shutdown()
        }catch (e:Exception){
            e.printStackTrace()
            FirebaseCrash.report(e)
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
                FirebaseCrash.report(e)
            }
            tts = null
            return
        }

        try {
            tts?.apply {
                language = Locale.getDefault()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    speak("", TextToSpeech.QUEUE_FLUSH, null, "")
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
            FirebaseCrash.report(e)
        }
    }
}