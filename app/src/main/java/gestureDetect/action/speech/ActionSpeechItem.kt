package gestureDetect.action.speech

import android.content.Context
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
    private var tts: TextToSpeech? = null

    fun isSpeechSupport():Boolean{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    override fun onCreate(context: Context): Boolean {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
        return super.onCreate(context)
    }

    fun doSpeech(context: Context, value: String): Boolean
    {
        action.vibrate(context)

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

    //  TTS init
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