package gesture

import android.content.Context
import android.graphics.drawable.Drawable
import ru.vpro.kernelgesture.R
import android.speech.tts.TextToSpeech
import android.util.Log
import java.text.MessageFormat
import java.util.*
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.content.Context.BATTERY_SERVICE




class GestureAction {

    companion object {
        private val allActions = arrayOf(
                ActionScreenOn(),
                GoogleNow(),
                ActionSpeechTime(),
                BatterySpeech(),
                WebBrowser(),
                CameraAction()
        )

        fun init(context: Context)
                = allActions.forEach { it.init(context) }

        fun getAction(context: Context, action: String): ActionItem?
                = allActions.firstOrNull { it.isAction(context, action) }

        fun getActions(): Array<ActionItem>
                = allActions
    }

    interface ActionItem {
        fun init(context: Context) {}
        fun action(): String
        fun isAction(context: Context, action: String): Boolean = action == action()
        fun name(context: Context): String = action()
        fun icon(context: Context): Drawable = context.getDrawable(android.R.color.transparent)
        fun run(context: Context): Boolean
    }


    /**
     * Screen ON
     */
    class ActionScreenOn : ActionItem {
        override fun action(): String = "screen.on"

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

    interface ActionItemSpeech : ActionItem, TextToSpeech.OnInitListener {
        companion object {
            var tts: TextToSpeech? = null
        }

        fun doSpeech(context: Context, value: String): Boolean
        {
            init(context)

            GestureService.UI.vibrate(context)
            GestureService.UI.playNotifyToEnd(context)

            tts?.language = Locale.getDefault()
            tts?.speak(value, TextToSpeech.QUEUE_ADD, null)

            return false
        }

        override fun init(context: Context) {
            if (tts != null) return
            tts = TextToSpeech(context, this)
        }

        override fun onInit(status: Int)
        {
            if (status != TextToSpeech.SUCCESS) {
                tts = null
                return
            }

            tts?.speak("", TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    /**
     * Time speech action
     */
    class ActionSpeechTime : ActionItemSpeech {
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

    /**
     * Speech battery percent
     */
    class BatterySpeech : ActionItemSpeech
    {
        override fun action(): String
                = "speech.battery"

        override fun name(context: Context): String
                = context.getString(R.string.ui_action_speech_battery)

        override fun icon(context: Context): Drawable
                = context.getDrawable(R.drawable.icon_speech_battery)

        override fun run(context: Context): Boolean
        {
            val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val prefix = context.getString(R.string.ui_action_battery)

            return doSpeech(context, "$prefix $batLevel%")
        }
    }

    /**
     * Action OK Google
     */
    class GoogleNow:ActionItem{
        override fun action(): String = "google.ok"

        override fun name(context: Context): String
                = context.getString(R.string.ui_ok_google)

        override fun icon(context: Context): Drawable
                = context.getDrawable(R.drawable.icon_ok_google)

        override fun run(context: Context): Boolean
        {
            GestureService.UI.screenON(context)
            GestureService.UI.screenUnlock(context)
            val googleNowIntent = Intent("android.intent.action.VOICE_ASSIST")
            return GestureService.UI.startNewActivity(context, googleNowIntent)
        }
    }
    /**
     * Action common class
     */
    interface ActionApp:ActionItem
    {
        var applicationInfo: ApplicationInfo?

        override fun action(): String
                = applicationInfo?.packageName ?: ""

        override fun name(context: Context): String
                = context.packageManager.getApplicationLabel(applicationInfo).toString()

        override fun icon(context: Context): Drawable {
            if (applicationInfo == null) return super.icon(context)
            return context.packageManager.getApplicationIcon(applicationInfo)
        }

        override fun run(context: Context): Boolean
        {
            if (applicationInfo == null) return false

            GestureService.UI.screenON(context)
            GestureService.UI.screenUnlock(context)

            return GestureService.UI.startNewActivity(context, applicationInfo!!.packageName)
        }
    }
    /**
     * Action default browser
     */
    class WebBrowser:ActionApp
    {
        override var applicationInfo:ApplicationInfo? = null

        override fun action(): String
                = if (applicationInfo?.packageName != null) "application.browser" else ""

        override fun init(context: Context)
        {
            val browserIntent = Intent("android.intent.action.VIEW", Uri.parse("http://"))
            val resolveInfo = context.packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)

            // This is the default browser's packageName
            applicationInfo = resolveInfo.activityInfo.applicationInfo
        }

        override fun name(context: Context): String = context.getString(R.string.ui_web_browser)
    }
    /**
     * Action default browser
     */
    class CameraAction:ActionApp
    {
        override var applicationInfo:ApplicationInfo? = null

        override fun action(): String
                = if (applicationInfo?.packageName != null) "application.camera" else ""

        override fun init(context: Context)
        {
            val browserIntent = Intent("android.media.action.IMAGE_CAPTURE")
            val resolveInfo = context.packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)

            // This is the default browser's packageName
            applicationInfo = resolveInfo.activityInfo.applicationInfo
        }

        override fun name(context: Context): String
                = context.getString(R.string.ui_action_camera)
    }
}
