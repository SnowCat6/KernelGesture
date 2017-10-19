package gestureDetect

import SuperSU.ShellSU
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import gestureDetect.action.ActionFlashlight
import gestureDetect.action.ActionItem
import gestureDetect.action.application.*
import gestureDetect.action.music.ActionMusicNext
import gestureDetect.action.music.ActionMusicPlayPause
import gestureDetect.action.music.ActionMusicPrev
import gestureDetect.action.screen.ActionScreenOff
import gestureDetect.action.screen.ActionScreenOn
import gestureDetect.action.screen.ActionScreenUnlock
import gestureDetect.action.speech.ActionSpeechBattery
import gestureDetect.action.speech.ActionSpeechTime
import gestureDetect.tools.GestureHW
import gestureDetect.tools.GestureSettings
import io.reactivex.subjects.BehaviorSubject
import ru.vpro.kernelgesture.BuildConfig


class GestureAction(val su : ShellSU = ShellSU())
{
    private val allActions = listOf(
            ActionScreenOn(this),
            ActionScreenUnlock(this),
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

    val rxUpdateItems = BehaviorSubject.createDefault(allActions)

    fun notifyChanged(action: ActionItem) {
        rxUpdateItems.onNext(allActions)
    }

    fun onCreate(context: Context)
    {
        allActions.forEach { it.onCreate(context) }
    }

    fun onStart() {
       allActions.forEach { it.onStart() }
    }
    fun onStop(){
        allActions.forEach { it.onStop() }
    }

    fun close(){
        allActions.forEach { it.close() }
    }

    fun getAction(context: Context, action: String?): ActionItem?
            = action?.let {
                allActions.find {  action.isNotEmpty() && it.isAction(context, action) }
            }

    fun getActions(context: Context): List<ActionItem>
            = allActions//.filter { it.action(context)?.isNotEmpty() == true }

    fun onGestureEvent(context: Context, gestureKey: String):Boolean
    {
        if (BuildConfig.DEBUG) {
            Log.d("Gesture action", gestureKey)
        }

        val settings = GestureSettings(context)
        var action:String? = settings.getAction(gestureKey)

        if ((action == null || action.isEmpty()) &&
                settings.getEnable("GESTURE_DEFAULT_ACTION"))
        {
            action = settings.getAction("GESTURE_DEFAULT_ACTION")
        }
        if (action == null || action.isEmpty()) return false

        try {
            getAction(context, action)?.apply {
                return run(context)
            }
        }catch (e:Exception){
            e.printStackTrace()
            FirebaseCrash.report(e)
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
            val intent = context.packageManager
                    ?.getLaunchIntentForPackage(action) ?: return false
            screenON(context)
            screenUnlock(context)
            return startNewActivity(context, intent)
        }catch (e:Exception){}
        return false
    }

    fun startNewActivity(context: Context, packageName: String):Boolean
    {
        var intent = context.packageManager?.getLaunchIntentForPackage(packageName)
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
        val settings = GestureSettings(context)
        if (settings.getEnable("GESTURE_UNLOCK_SCREEN")) {
            GestureHW(context).screenUnlock()
        }
    }
    fun screenON(context: Context)
    {
        playNotify(context)
        vibrate(context)
        GestureHW(context).screenON()
    }
    private fun playNotify(context: Context):Boolean
    {
        getRingtone(context)?.apply {
            play()
            return false
        }
        return true
    }
    fun playNotifyToEnd(context: Context):Boolean
    {
        getRingtone(context)?.apply {
            play()
            while(isPlaying) Thread.sleep(50)
            return true
        }
        return false
    }
    fun vibrate(context: Context) {
        val settings = GestureSettings(context)
        if (settings.getEnable("GESTURE_VIBRATION")) {
            GestureHW(context).vibrate()
        }
    }

    fun getRingtone(context: Context) : Ringtone?
    {
        try {
            val settings = GestureSettings(context)
            val value = settings.getPreference()
                    ?.getString("GESTURE_NOTIFY", null)
            if (value == null || value.isEmpty()) return null

            val notify = Uri.parse(value)
            if (notify != null)
                return RingtoneManager.getRingtone(context, notify)

        }catch (e:Exception){  }
        return null
    }

    companion object {

        private var actions : GestureAction? = null
        fun getInstance(context: Context)
            = actions ?: GestureAction().apply {
                actions = this
                onCreate(context)
            }
    }
}
