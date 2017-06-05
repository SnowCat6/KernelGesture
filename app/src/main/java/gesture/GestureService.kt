package gesture

import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log
import android.content.Intent
import android.content.BroadcastReceiver
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.ContactsContract
import kotlin.concurrent.thread

class GestureService() : Service()
{
    companion object
    {
        val gesture = GestureDetect()
        var bRunning = false
    }
    fun startGesture()
    {
        if (bRunning) return
        bRunning = true
        gesture.lock = false

        thread {
            while (!gesture.lock) {
                val ev = gesture.waitGesture(this) ?: break
                if (onGestureEvent(ev)) break
            }
            bRunning = false
            gesture.lock = true
        }
    }
    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(onScreenIntent, intentFilter)
        super.onStartCommand(intent, flags, startId)

        if (!GestureDetect.isScreenOn(this)){
            startGesture()
        }

        return START_STICKY
    }

    val onScreenIntent = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(ContentValues.TAG, Intent.ACTION_SCREEN_OFF)
                    gesture.lock = false
                    startGesture()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(ContentValues.TAG, Intent.ACTION_SCREEN_ON)
                    gesture.lock = true
                }
            }
        }
    }

    override fun stopService(name: Intent?): Boolean {
        gesture.lock = true
        unregisterReceiver(onScreenIntent)
        return super.stopService(name)
    }

    fun onGestureEvent(gestureKey:String):Boolean
    {
        Log.d("Gesture action", gestureKey)

        if (!GestureDetect.getAllEnable(this)) return false
        if (!GestureDetect.getEnable(this, gestureKey)) return false

        var action:String? = GestureDetect.getAction(this, gestureKey)

        if (action == null && GestureDetect.getEnable(this, "GESTURE_DEFAULT_ACTION"))
        {
            action = GestureDetect.getAction(this, "GESTURE_DEFAULT_ACTION")
        }
        if (action == null) return false

        when(action){
            "screen.on" ->{
                screenON()
                return true
            }
            "phone" -> {
                //For the dial pad
                screenON()
                return startNewActivity(Intent(Intent.ACTION_DIAL, null))
            }
            "phone.contacts" -> {
                //For the contacts (viewing them)
                screenON()
                return startNewActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
            }
        }

        try {
            screenON()
            val intent = packageManager.getLaunchIntentForPackage(action)
            if (intent == null) return false
            return startNewActivity(intent)
        }catch (e:Exception){}
        return false
    }

    fun startNewActivity(context: Context, packageName: String) {
        var intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=" + packageName)
        }
        startNewActivity(intent)
    }
    fun startNewActivity(intent: Intent):Boolean
    {
        try {
            GestureDetect.screenUnlock(this)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return true
        }catch (e:Exception){}
        return false
    }
    private fun screenON()
    {
        if (GestureDetect.getEnable(this, "GESTURE_VIBRATION")) GestureDetect.vibrate(this)

        GestureDetect.screenON(this)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        try {
            val notify = Uri.parse(sharedPreferences.getString("GESTURE_NOTIFY", null))
            if (notify != null) GestureDetect.playNotify(this, notify)
        }catch (e:Exception){}
    }
}
