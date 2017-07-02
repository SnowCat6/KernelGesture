package gesture

import android.app.KeyguardManager
import android.app.Notification
import android.app.Service
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import ru.vpro.kernelgesture.BuildConfig
import ru.vpro.kernelgesture.R
import java.lang.Thread.MAX_PRIORITY
import kotlin.concurrent.thread

class GestureService : Service(), SensorEventListener {

    companion object
    {
        var keyguardLock: KeyguardManager.KeyguardLock? = null
    }

    private var bRunning = false

    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null

    private var gestureDetector:GestureDetect? = null
    private var gestureActions:GestureAction? = null

    /************************************/
    /*
    GESTURE DETECT
     */
    fun startGesture()
    {
        //  Disable run thread if gestures not use
        if (!GestureDetect.getAllEnable(this)){
            gestureDetector?.close()
            gestureDetector = null
            bRunning = false
            return
        }
        // If thread ruined return back
        if (bRunning) return
        bRunning = true

        thread {

            val bForeground = true

            if (bForeground) {
                /*****************************/
                val builder = Notification.Builder(this)
                        .setSmallIcon(R.drawable.icon_screen_on)
                        .setContentTitle(getString(R.string.ui_service))

                val notification = builder.build()
                startForeground(777, notification)
                /*****************************/
            }

            if (gestureActions == null)
                gestureActions = GestureAction(this)

            if (gestureDetector == null)
                gestureDetector = GestureDetect(this)

            val actions = gestureActions!!
            val gesture = gestureDetector!!

            gesture.enable(true)

            actions.onStart()
            gesture.lock = false

            //  If proximity sensor used, register event
            val bProximityEnable = GestureDetect.getEnable(this, "GESTURE_PROXIMITY")
            gesture.isNear = false
            if (bProximityEnable) {
                mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
            }

            //  Main gesture loop
            //  Wait gesture while live
            while (gesture.lock != true)
            {
                val ev = gesture.waitGesture() ?: break
                try {
                    if (actions.onGestureEvent(ev) == true) break
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }

            gesture.lock = true

            //  Unregister even if this need
            if (bProximityEnable) {
                mSensorManager?.unregisterListener(this)
            }
            actions.onStop()

            if (bForeground) {
                /*****************************/
                stopForeground(true)
                /*****************************/
            }

            //  Mark thread stopped
            bRunning = false

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
        gestureDetector?.isNear = event.values[0].toInt() == 0
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

        if (gestureDetector == null)
            gestureDetector = GestureDetect(this)

        //  Enable/disable gestures on start service
        gestureDetector?.enable(GestureDetect.getAllEnable(this))

        //  If screen off - run thread
        if (!GestureAction.HW.isScreenOn(this)){
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
                    gestureDetector?.lock = true
                }
            }
        }
    }

    override fun onDestroy()
    {
        gestureDetector?.close()
        gestureDetector = null

        gestureActions?.close()
        gestureActions = null

        unregisterReceiver(onScreenIntent)

        super.onDestroy()
    }
}
