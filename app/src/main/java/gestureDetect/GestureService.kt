package gestureDetect

import SuperSU.ShellSU
import android.app.IntentService
import android.app.KeyguardManager
import android.app.Notification
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import ru.vpro.kernelgesture.BuildConfig
import ru.vpro.kernelgesture.R

class GestureService : IntentService("AllKernelGesture"), SensorEventListener {

    companion object {
        var keyguardLock: KeyguardManager.KeyguardLock? = null
    }

    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null

    private var gestureDetector:GestureDetect? = null
    private var gestureActions:GestureAction? = null
    private val su = ShellSU()

    /************************************/
    /*
    GESTURE DETECT
     */

    override fun onHandleIntent(intent: Intent?)
    {
        setServiceForeground(!GestureAction.HW.isScreenOn(this))

        if (gestureActions == null)
            gestureActions = GestureAction(this)

        if (gestureDetector == null)
            gestureDetector = GestureDetect(this)

        val actions = gestureActions!!
        val gesture = gestureDetector!!

        su.checkRootAccess()
        gesture.enable(true)

        actions.onStart()
        gesture.disabled = false

        //  If proximity sensor used, register event
        val bProximityEnable = GestureDetect.getEnable(this, "GESTURE_PROXIMITY")
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
    SENSOR
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    override fun onSensorChanged(event: SensorEvent)
    {
        // If registered use proximity - change value detector
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        gestureDetector?.isNearProximity = event.values[0].toInt() == 0
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
        gestureDetector?.screenOnMode = GestureAction.HW.isScreenOn(this)

        return super.onStartCommand(intent, flags, startId)
    }

    val onScreenIntent = object : BroadcastReceiver()
    {
        //  Events for screen on and screen off
        override fun onReceive(context: Context, intent: Intent)
        {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    keyguardLock?.reenableKeyguard()
                    setServiceForeground(true)
                    gestureDetector?.screenOnMode = false
                    gestureActions?.screenOnMode = false
                }
                Intent.ACTION_SCREEN_ON -> {
                    setServiceForeground(false)
                    gestureDetector?.screenOnMode = true
                    gestureActions?.screenOnMode = true
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
