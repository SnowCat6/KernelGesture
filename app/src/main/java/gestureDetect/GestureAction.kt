package gestureDetect

import SuperSU.ShellSU
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import gestureDetect.action.*
import gestureDetect.action.application.*
import gestureDetect.action.music.ActionMusicNext
import gestureDetect.action.music.ActionMusicPlayPause
import gestureDetect.action.music.ActionMusicPrev
import gestureDetect.action.speech.ActionSpeechBattery
import gestureDetect.action.speech.ActionSpeechTime
import ru.vpro.kernelgesture.BuildConfig


class GestureAction(val context:Context)
{
    private val allActions = arrayOf(
            ActionScreenOn(this),
            ActionScreenOff(this),
            ActionGoogleNow(this),
            ActionSpeechTime(this),
            ActionSpeechBattery(this),
            ActionFlashlight(this),
            ActionMusicPlayPause(this),
            ActionMusicPrev(this),
            ActionMusicNext(this),
            ActionDialer(this),
            ActionContacts(this),
            ActionWebBrowser(this),
            ActionMail(this),
            ActionCamera(this)
    )
    val hw = GestureHW(context)
    val su = ShellSU()
    val settings = GestureSettings(context)

    init {
        onDetect()
    }

    private var _screenOnMode = false
    var screenOnMode
        get() = _screenOnMode
        set(value){
            if (value != screenOnMode){
                onStop()
                _screenOnMode = value
                onStart()
            }
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
        if (BuildConfig.DEBUG) {
            Log.d("Gesture action", gestureKey)
        }

        var action:String? = settings.getAction(gestureKey)

        if ((action == null || action.isEmpty()) && settings.getEnable("GESTURE_DEFAULT_ACTION")){
            action = settings.getAction("GESTURE_DEFAULT_ACTION")
        }
        if (action == null || action.isEmpty()) return false

        try {
            getAction(action)?.apply {
                return run()
            }
        }catch (e:Exception){
            e.printStackTrace()
            return false
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
            val intent = context.packageManager.getLaunchIntentForPackage(action) ?: return false
            screenON()
            screenUnlock()
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
        if (settings.getEnable("GESTURE_UNLOCK_SCREEN")) {
            GestureService.keyguardLock?.disableKeyguard()
        }
    }
    fun screenON()
    {
        playNotify()
        vibrate()
        hw.screenON()
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
        if (settings.getEnable("GESTURE_VIBRATION"))
            hw.vibrate()
    }

}
