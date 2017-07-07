package gestureDetect

import SuperSU.ShellSU
import android.app.IntentService
import android.app.KeyguardManager
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import ru.vpro.kernelgesture.BuildConfig
import ru.vpro.kernelgesture.R

class GestureService :
//        Service(),
        IntentService("AnyKernelGesture"),
        SensorEventListener
{
    companion object {
        var keyguardLock: KeyguardManager.KeyguardLock? = null
    }

    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null

    private var gestureDetector:GestureDetect? = null
    private var gestureActions:GestureAction? = null

    val su = ShellSU()
    /************************************/
    /*
    GESTURE DETECT
     */
    override fun onHandleIntent(intent: Intent?)
    {
        su.checkRootAccess(this)
        val hw = GestureHW(this)

        setServiceForeground(!hw.isScreenOn())
        val settings = GestureSettings(this)

        if (gestureActions == null)
            gestureActions = GestureAction(this)

        if (gestureDetector == null)
            gestureDetector = GestureDetect(this)

        val actions = gestureActions!!
        val gesture = gestureDetector!!

        actions.onStart()
        gesture.disabled = false

        //  If proximity sensor used, register event
        val bProximityEnable = settings.getEnable( "GESTURE_PROXIMITY")
        gesture.isNearProximity = false
        if (bProximityEnable) {
            mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
        }

        //  Main gesture loop
        //  Wait gesture while live
        while (!gesture.disabled)
        {
            val ev = gesture.waitGesture() ?: break
            try {
                actions.onGestureEvent(ev)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }

        //  Unregister even if this need
        if (bProximityEnable) {
            mSensorManager?.unregisterListener(this)
        }
        actions.onStop()

        setServiceForeground(false)
    }
    var bForeground = false
    fun setServiceForeground(bSetForeground:Boolean)
    {
        if (bForeground == bSetForeground)
            return

        bForeground = bSetForeground

        if (BuildConfig.DEBUG){
            Log.d("Service foreground", bForeground.toString())
        }

        if (bForeground) {
            val builder = Notification.Builder(this)
                    .setSmallIcon(R.drawable.icon_screen_on)
                    .setContentTitle(getString(R.string.ui_service))

            val notification = builder.build()
            startForeground(777, notification)
            /*****************************/
        }else{
            /*****************************/
            stopForeground(true)
            /*****************************/
        }
    }
    /************************************/
    /*
    SERVICE
     */
    override fun onBind(intent: Intent): IBinder?
    {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    override fun onCreate()
    {
        if (BuildConfig.DEBUG){
            Log.d("Start service", "**************************")
        }

        su.checkRootAccess(this)
        val hw = GestureHW(this)
        //  Get sensor devices
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardLock = keyguardManager.newKeyguardLock("KernelGesture")

        if (gestureDetector == null)
            gestureDetector = GestureDetect(this)

        //  Enable/disable gestures on start service
        gestureDetector?.enable(true)
        gestureDetector?.screenOnMode = hw.isScreenOn()

        //  Register screen activity event
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(onEventIntent, intentFilter)
        LocalBroadcastManager.getInstance(this).registerReceiver(onEventIntent, IntentFilter(GestureSettings.EVENT_ENABLE))

        super.onCreate()
    }

    override fun onDestroy()
    {
        closeService()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        closeService()
        super.onTaskRemoved(rootIntent)
    }

    fun closeService()
    {
        if (BuildConfig.DEBUG){
            Log.d("Stop service", "**************************")
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEventIntent)
        unregisterReceiver(onEventIntent)

        gestureDetector?.close()
        gestureDetector = null

        gestureActions?.close()
        gestureActions = null
    }
    /************************************/
    /*
    SENSOR events
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onSensorChanged(event: SensorEvent)
    {
        // If registered use proximity - change value detector
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        gestureDetector?.isNearProximity = event.values[0].toInt() == 0
    }
    /**
     * SCREEN Events
     */
    val onEventIntent = object : BroadcastReceiver()
    {
        //  Events for screen on and screen off
        override fun onReceive(context: Context, intent: Intent)
        {
            when (intent.action) {
                //  Screen ON
                Intent.ACTION_SCREEN_OFF -> {
                    keyguardLock?.reenableKeyguard()
                    setServiceForeground(true)
                    gestureDetector?.screenOnMode = false
                    gestureActions?.screenOnMode = false
                }
                //  Screen OFF
                Intent.ACTION_SCREEN_ON -> {
                    setServiceForeground(false)
                    gestureDetector?.screenOnMode = true
                    gestureActions?.screenOnMode = true
                }
                //  Enable gestures
                GestureSettings.EVENT_ENABLE ->{
                    val key = intent.getSerializableExtra("key") as String
                    if (key != "GESTURE_ENABLE") return

                    val bEnable = intent.getSerializableExtra("value") as Boolean? ?: false
                    if (bEnable)
                    {
                        gestureActions?.onDetect()
                        gestureDetector?.enable(true)
                    }else {
                        stopSelf()
                    }
                }
            }
        }
    }
}
