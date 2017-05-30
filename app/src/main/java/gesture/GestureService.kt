package gesture

import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log
import android.content.Intent
import android.content.BroadcastReceiver
import android.net.Uri
import android.provider.ContactsContract




class GestureService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    val gesture = GestureDetect()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        gesture.onGesture += { onGestureEvent(it) }
//        gesture.onGesture.invoke("KEY_UP")

        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(onScreenIntent, intentFilter)

        return START_STICKY
    }

    val onScreenIntent: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(ContentValues.TAG, Intent.ACTION_SCREEN_OFF)
                    gesture.startWait()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(ContentValues.TAG, Intent.ACTION_SCREEN_ON)
                    gesture.stopWait()
                }
            }
        }
    }

    override fun stopService(name: Intent?): Boolean {
        unregisterReceiver(onScreenIntent)
        return super.stopService(name)
    }

    fun onGestureEvent(it:String)
    {
        if (!GestureDetect.getAllEnable(this)) return
        if (!GestureDetect.getEnable(this, it)) return
        val action = GestureDetect.getAction(this, it) ?: return

        when(action){
            "screen.on" ->{
                gesture.screenON(baseContext)
            }
            "phone" -> {
                //For the dial pad
                startNewActivity(this, Intent(Intent.ACTION_DIAL, null))
            }
            "phone.contacts" -> {
                //For the contacts (viewing them)
                startNewActivity(this, Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
            }
        }
    }

    fun startNewActivity(context: Context, packageName: String) {
        var intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=" + packageName)
        }
        startNewActivity(context, intent)
    }
    fun startNewActivity(context: Context, intent: Intent)
    {
        gesture.screenON(baseContext)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
