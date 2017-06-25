package gesture

import android.app.KeyguardManager
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
import kotlin.concurrent.thread
import android.hardware.SensorEvent
import android.hardware.SensorManager
import ru.vpro.kernelgesture.BuildConfig
import java.lang.Thread.MAX_PRIORITY


class GestureService() : Service(), SensorEventListener
{
    companion object
    {
        private var bRunning = false

        private var ringtone:Ringtone? = null

        private var mSensorManager: SensorManager? = null
        private var mProximity: Sensor? = null

        var keyguardLock: KeyguardManager.KeyguardLock? = null
    }
    /************************************/
    /*
    GESTURE DETECT
     */
    fun startGesture()
    {
        val gesture = GestureDetect.getInstance(this)
        gesture.lock = false

        // If thread ruined return back
        if (bRunning) return

        //  Disable run thread if gestures not use
        if (!GestureDetect.getAllEnable(this)){
            gesture.close()
            return
        }
        GestureAction.init(this)

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
        }.priority = MAX_PRIORITY
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
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val gesture = GestureDetect.getInstance(this)
        gesture.isNear = event.values[0].toInt() == 0
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

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardLock = keyguardManager.newKeyguardLock("KernelGesture")

        GestureAction.init(this)

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
                    if (BuildConfig.DEBUG) {
                        Log.d(ContentValues.TAG, Intent.ACTION_SCREEN_OFF)
                    }
                    keyguardLock?.reenableKeyguard()
                    startGesture()
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(ContentValues.TAG, Intent.ACTION_SCREEN_ON)
                    }
                    val gesture = GestureDetect.getInstance(context)
                    gesture.lock = true
                }
            }
        }
    }

    override fun stopService(name: Intent?): Boolean
    {
        val gesture = GestureDetect.getInstance(this)

        gesture.lock = true
        gesture.close()
        unregisterReceiver(onScreenIntent)
        return super.stopService(name)
    }

    fun onGestureEvent(gestureKey:String):Boolean
    {
        var action:String? = GestureDetect.getAction(this, gestureKey)

        if ((action == null || action.isEmpty()) && GestureDetect.getEnable(this, "GESTURE_DEFAULT_ACTION")){
            action = GestureDetect.getAction(this, "GESTURE_DEFAULT_ACTION")
        }
        if (action == null || action.isEmpty()) return false

        if (BuildConfig.DEBUG) {
            Log.d("Gesture action", gestureKey)
        }

        val a = GestureAction.getAction(this, action)
        if (a != null) return a.run(this)
/*
            "screen.on" ->{
            "player.next" -> {
            "player.prev" -> {
            "player.playPause" -> {
            "browser" ->{
            "speech.time"->{
            "okgoogle" ->{
*/
        try {
            UI.screenON(this)
            UI.screenUnlock(this)
            val intent = packageManager.getLaunchIntentForPackage(action) ?: return false
            return UI.startNewActivity(this, intent)
        }catch (e:Exception){}
        return false
    }

    object UI
    {
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
                Companion.keyguardLock?.disableKeyguard()
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
