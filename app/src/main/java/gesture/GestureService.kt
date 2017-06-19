package gesture

import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log
import android.content.Intent
import android.content.BroadcastReceiver
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.ContactsContract
import kotlin.concurrent.thread
import android.widget.Toast
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorManager
import ru.vpro.kernelgesture.BuildConfig


class GestureService() : Service(), SensorEventListener
{
    companion object
    {
        private val gesture = GestureDetect()
        private var bRunning = false

        private var ringtone:Ringtone? = null

        private var mSensorManager: SensorManager? = null
        private var mProximity: Sensor? = null
    }
    /************************************/
    /*
    GESTURE DETECT
     */
    fun startGesture()
    {
        // If thread ruined return back
        if (bRunning) return
        //  Disable run thread if gestures not use
        if (!GestureDetect.getAllEnable(this)) return

        bRunning = true
        gesture.lock = false

        thread {
            //  Preload notify
            try {
                ringtone = null
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                val notify = Uri.parse(sharedPreferences.getString("GESTURE_NOTIFY", null))
                if (notify != null) ringtone = RingtoneManager.getRingtone(this, notify)
            }catch (e:Exception){}

            //  If proximity sensor used, register event
            val bProximityEnable = GestureDetect.getEnable(this, "GESTURE_PROXIMITY")
            gesture.isNear = false
            if (bProximityEnable) {
                mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
            }

            //  Main gesture loop
            //  Wait gesture while live
            while (!gesture.lock) {
                val ev = gesture.waitGesture(this) ?: break
                if (onGestureEvent(ev)) break
            }
            //  Mark thread stopped
            bRunning = false
            gesture.lock = true

            //  Unregister even if this need
            if (bProximityEnable) {
                mSensorManager?.unregisterListener(this)
            }
        }
    }
    /************************************/
    /*
    SENSOR
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    override fun onSensorChanged(event: SensorEvent)
    {
        // If registered use proximity - change value detector
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
             gesture.isNear = event.values[0].toInt() == 0
        }
    }
    /************************************/
    /*
    SERVICE
     */
    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        super.onStartCommand(intent, flags, startId)

        //  Register screen activity event
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(onScreenIntent, intentFilter)

        //  Get sensor devices
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        //  If screen off - run thread
        if (!GestureDetect.isScreenOn(this)){
            startGesture()
        }

        return START_STICKY
    }

    val onScreenIntent = object : BroadcastReceiver()
    {
        //  Events for screen on and screen off
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

    override fun stopService(name: Intent?): Boolean
    {
        gesture.lock = true
        unregisterReceiver(onScreenIntent)
        return super.stopService(name)
    }

    fun onGestureEvent(gestureKey:String):Boolean
    {
        if (BuildConfig.DEBUG) {
            Log.d("Gesture action", gestureKey)
        }

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
            val intent = packageManager.getLaunchIntentForPackage(action) ?: return false
            return startNewActivity(intent)
        }catch (e:Exception){}
        return false
    }

    fun startNewActivity(context: Context, packageName: String) {
        var intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=$packageName")
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
        ringtone?.play()
        if (GestureDetect.getEnable(this, "GESTURE_VIBRATION")) GestureDetect.vibrate(this)
        GestureDetect.screenON(this)
    }
}
