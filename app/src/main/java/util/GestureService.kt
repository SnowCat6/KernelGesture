package util

import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log

class GestureService : Service() {

    val gesture = GestureDetect()
    var onScreenIntent: BroadcastReceiver? = null

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        gesture.onGesture += {
            Log.d("GestureDetect command", it)
            gesture.screenON(baseContext)
        }

        if (onScreenIntent == null) {
            val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF)

            onScreenIntent = object : BroadcastReceiver() {
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

            registerReceiver(onScreenIntent, intentFilter)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun stopService(name: Intent?): Boolean {
        unregisterReceiver(onScreenIntent)
        onScreenIntent = null
        return super.stopService(name)
    }
}
