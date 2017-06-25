package gesture

import android.content.Context
import android.graphics.drawable.Drawable
import ru.vpro.kernelgesture.R
import android.speech.tts.TextToSpeech
import android.util.Log
import java.text.MessageFormat
import java.util.*


class GestureAction
{
    interface ActionItem
    {
        fun init(context: Context){}
        fun action():String
        fun isAction(context: Context, action: String): Boolean = action == action()
        fun name(context: Context): String
        fun icon(context: Context): Drawable =  context.getDrawable(android.R.color.transparent)
        fun run(context: Context): Boolean
    }

    companion object
    {
        private val allActions = arrayOf<ActionItem>(ActionScreenOn(), ActionSpeechTime())

        fun init(context: Context)
            = allActions.forEach { it.init(context) }

        fun getAction(context:Context, action:String):ActionItem?
                = allActions.firstOrNull { it.isAction(context, action)  }

        fun getActions():Array<ActionItem> = allActions
    }

    /**
     * Screen ON
     */
    class ActionScreenOn :ActionItem
    {
        override fun action():String =  "screen.on"

        override fun name(context: Context): String
                = context.getString(R.string.ui_screen_on)

        override fun icon(context: Context): Drawable
                = context.getDrawable(R.drawable.icon_screen_on)

        override fun run(context: Context): Boolean {
            GestureService.UI.screenON(context)
            return true
        }
    }

    /**
     * Common speech class
     */

    interface ActionItemSpeech:ActionItem, TextToSpeech.OnInitListener
    {
        companion object{
            var tts:TextToSpeech? = null
            var values = arrayOf<String>()
            var isInit = false
        }

        fun doSpeech(context: Context, value:String):Boolean
        {
            init(context)
            GestureService.UI.vibrate(context)
            GestureService.UI.playNotifyToEnd(context)
            Thread.sleep(200)

            if (isInit){
                doIntSpeak(value)
            }else {
                values += value
            }
            return false
        }

        override fun init(context: Context)
        {
            if (tts == null) {
                tts = TextToSpeech(context, this)
            }
        }

        override fun onInit(status: Int)
        {
            if(status != TextToSpeech.SUCCESS)
            {
                values = emptyArray()
                tts = null
                return
            }

            isInit = true
            values.forEach {
                doIntSpeak(it)
            }
            values = emptyArray()
        }
        fun doIntSpeak(value:String)
        {
            if (tts == null) return

            tts?.language = Locale.getDefault()
            tts?.speak(value, TextToSpeech.QUEUE_FLUSH, null)
            while (tts?.isSpeaking == true){
                Thread.sleep(100)
            }
        }
    }

    /**
     * Time speech action
     */
    class ActionSpeechTime():ActionItemSpeech
    {
        override fun action(): String
                = "speech.time"

        override fun name(context: Context): String
                = context.getString(R.string.ui_speech_time)

        override fun icon(context: Context): Drawable {
            return context.getDrawable(R.drawable.icon_speech_time)
        }

        override fun run(context: Context): Boolean
        {
            val result = MessageFormat.format("{0,time,short}", Date())
            Log.d("Time is", result)
            return doSpeech(context, result)
        }
    }
}
