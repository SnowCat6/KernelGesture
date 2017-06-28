package gesture

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import gesture.action.*
import ru.vpro.kernelgesture.BuildConfig


class GestureAction(val context:Context)
{
    private val allActions = arrayOf(
            ActionScreenOn(this),
            ActionGoogleNow(this),
            ActionSpeechTime(this),
            ActionSpeechBattery(this),
            ActionWebBrowser(this),
            ActionCamera(this)
    )

    fun onStart() {
        //  Preload notify
        try {
            UI.ringtone = null
            val value = PreferenceManager.getDefaultSharedPreferences(context).getString("GESTURE_NOTIFY", null)
            if (value != null && !value.isEmpty()) {
                val notify = Uri.parse(value)
                if (notify != null) UI.ringtone = RingtoneManager.getRingtone(context, notify)
            }
        }catch (e:Exception){}

       allActions.forEach { it.onStart() }
    }
    fun onStop(){
        allActions.forEach { it.onStop() }
    }

    fun close(){
        allActions.forEach { it.close() }
    }

    fun getAction(action: String): ActionItem?
            = allActions.firstOrNull { it.isAction(action) }

    fun getActions(): Array<ActionItem>
            = allActions

    fun onGestureEvent(gestureKey:String):Boolean
    {
        var action:String? = GestureDetect.getAction(context, gestureKey)

        if ((action == null || action.isEmpty()) && GestureDetect.getEnable(context, "GESTURE_DEFAULT_ACTION")){
            action = GestureDetect.getAction(context, "GESTURE_DEFAULT_ACTION")
        }
        if (action == null || action.isEmpty()) return false

        if (BuildConfig.DEBUG) {
            Log.d("Gesture action", gestureKey)
        }

        val a = getAction(action)
        if (a != null) return a.run()
/*
            "screen.on"
            "player.next"
            "player.prev"
            "player.playPause"
            "browser"
            "speech.time"
            "okgoogle"
            "phone.call.#############"
*/
        try {
            UI.screenON(context)
            UI.screenUnlock(context)
            val intent = context.packageManager.getLaunchIntentForPackage(action) ?: return false
            return UI.startNewActivity(context, intent)
        }catch (e:Exception){}
        return false
    }

    object UI
    {
        var ringtone: Ringtone? = null

        fun startNewActivity(context: Context, packageName: String):Boolean
        {
            var intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent == null) {
                // Bring user to the market or let them choose an app?
                intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("market://details?id=$packageName")
            }
            return startNewActivity(context, intent)
        }
        fun startNewActivity(context: Context, intent: Intent):Boolean
        {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }catch (e:Exception){
                return false
            }
            return true
        }
        fun screenUnlock(context: Context)
        {
            if (GestureDetect.getEnable(context, "GESTURE_UNLOCK_SCREEN")) {
                GestureService.keyguardLock?.disableKeyguard()
            }
        }
        fun screenON(context: Context)
        {
            playNotify(context)
            vibrate(context)
            GestureDetect.screenON(context)
        }
        fun playNotify(context: Context):Boolean{
            if (ringtone == null) return false
            ringtone?.play()
            return false
        }
        fun playNotifyToEnd(context: Context):Boolean{
            if (ringtone == null) return false
            ringtone?.play()
            while(ringtone?.isPlaying == true){
                Thread.sleep(100)
            }
            return false
        }
        fun vibrate(context: Context){
            if (GestureDetect.getEnable(context, "GESTURE_VIBRATION"))
                GestureDetect.vibrate(context)
        }
    }
}
