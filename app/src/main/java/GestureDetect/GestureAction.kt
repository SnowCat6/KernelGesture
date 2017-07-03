package GestureDetect

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.view.Display
import GestureDetect.action.*
import GestureDetect.action.application.*
import GestureDetect.action.speech.ActionSpeechBattery
import GestureDetect.action.speech.ActionSpeechTime
import ru.vpro.kernelgesture.BuildConfig


class GestureAction(val context:Context)
{
    private val allActions = arrayOf(
            ActionScreenOn(this),
            ActionGoogleNow(this),
            ActionSpeechTime(this),
            ActionSpeechBattery(this),
            ActionFlashlight(this),
            ActionDialer(this),
            ActionContacts(this),
            ActionWebBrowser(this),
            ActionMail(this),
            ActionCamera(this)
    )
    init {
        onDetect()
    }

    fun onDetect(){
        allActions.forEach { it.onDetect() }
    }

    fun onStart() {
        //  Preload notify
        try {
            ringtone = null
            val value = PreferenceManager.getDefaultSharedPreferences(context).getString("GESTURE_NOTIFY", null)
            if (value != null && !value.isEmpty()) {
                val notify = Uri.parse(value)
                if (notify != null) ringtone = RingtoneManager.getRingtone(context, notify)
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

    fun getAction(action: String?): ActionItem?
            = if (action == null) null else allActions.find {  action.isNotEmpty() && it.isAction(action) }

    fun getActions(): List<ActionItem>
            = allActions.filter { it.action().isNotEmpty() }

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

        getAction(action)?.apply {
            return run()
        }
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
            screenON()
            screenUnlock()
            val intent = context.packageManager.getLaunchIntentForPackage(action) ?: return false
            return startNewActivity(intent)
        }catch (e:Exception){}
        return false
    }

    var ringtone: Ringtone? = null

    fun startNewActivity(packageName: String):Boolean
    {
        var intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=$packageName")
        }
        return startNewActivity(intent)
    }
    fun startNewActivity(intent: Intent):Boolean
    {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }catch (e:Exception){
            return false
        }
        return true
    }
    fun screenUnlock()
    {
        if (GestureDetect.getEnable(context, "GESTURE_UNLOCK_SCREEN"))
            GestureService.keyguardLock?.disableKeyguard()
    }
    fun screenON()
    {
        playNotify()
        vibrate()
        HW.screenON(context)
    }
    fun playNotify():Boolean
    {
        ringtone?.apply {
            play()
            return false
        }
        return true
    }
    fun playNotifyToEnd():Boolean
    {
        ringtone?.apply {
            play()
            while(isPlaying) Thread.sleep(50)
            return true
        }
        return false
    }
    fun vibrate(){
        if (GestureDetect.getEnable(context, "GESTURE_VIBRATION"))
            HW.vibrate(context)
    }

    object HW{
        /*
            <uses-permission android:name="android.permission.WAKE_LOCK" />
        */
        fun screenON(context:Context)
        {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.FULL_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "KernelGesture")
            wakeLock.acquire(500)
        }
        fun powerON(context:Context)
        {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "KernelGestureCPU")
            wakeLock.acquire(1000)
        }
        /*
        <uses-permission android:name="android.permission.VIBRATE"/>
         */
        fun vibrate(context:Context){
            // Vibrate for 500 milliseconds
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(200)
        }

        fun isScreenOn(context: Context): Boolean
        {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            return dm.displays.any { it.state != Display.STATE_OFF }
        }
        /*
        <uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
         */
    }
}
