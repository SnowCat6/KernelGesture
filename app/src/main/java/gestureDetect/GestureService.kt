package gestureDetect

import SuperSU.ShellSU
import android.app.IntentService
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
import com.google.firebase.crash.FirebaseCrash
import gestureDetect.tools.GestureHW
import gestureDetect.tools.GestureSettings
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import ru.vpro.kernelgesture.BuildConfig
import ru.vpro.kernelgesture.R

class GestureService :
//        Service(),
        IntentService("AnyKernelGesture"),
        SensorEventListener
{
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null

    private var gestureDetector:GestureDetect? = null
    private var gestureActions:GestureAction? = null

    private val su = ShellSU()
    private var bForeground = false

    private val composites = CompositeDisposable()
    /************************************/
    /*
    GESTURE DETECT
     */
    override fun onHandleIntent(intent: Intent?)
    {
        su.checkRootAccess()
        val hw = GestureHW(this)

        setServiceForeground(!hw.isScreenOn())
        val settings = GestureSettings(this)

        val actions = gestureActions ?: GestureAction(this)
        gestureActions = actions

        val gesture = gestureDetector ?: GestureDetect(this)
        gestureDetector = gesture

        actions.onStart()
        gesture.bClosed = false

        //  If proximity sensor used, register event
        val bProximityEnable = settings.getEnable( "GESTURE_PROXIMITY")
        gesture.isNearProximity = false
        if (bProximityEnable) {
            mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
        }

        //  Main gesture loop
        //  Wait gesture while live
        while (!gesture.bClosed)
        {
            val ev = gesture.waitGesture() ?: break
            try {
                actions.onGestureEvent(ev)
            }catch (e:Exception){
                e.printStackTrace()
                FirebaseCrash.report(e)
            }
        }

        //  Unregister even if this need
        if (bProximityEnable) {
            mSensorManager?.unregisterListener(this)
        }
        actions.onStop()
        gesture.close()

        setServiceForeground(false)
    }
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
                    .setSmallIcon(R.drawable.icon_service)
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
            FirebaseCrash.setCrashCollectionEnabled(false)
        }

        if (BuildConfig.DEBUG){
            Log.d("Start service", "**************************")
        }

        su.checkRootAccess()
        val hw = GestureHW(this)
        //  Get sensor devices
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val gesture = gestureDetector ?: GestureDetect(this)
        gestureDetector = gesture

        //  Enable/disable gestures on start service
        gesture.enable(true)
        gesture.screenOnMode = hw.isScreenOn()

        //  Register screen activity event
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(onEventIntent, intentFilter)
//        LocalBroadcastManager.getInstance(this).registerReceiver(onEventIntent, IntentFilter(GestureSettings.EVENT_ENABLE))
        composites += GestureSettings.rxUpdateValue
                .filter { it.key ==  GestureSettings.GESTURE_ENABLE && it.value == false }
                .observeOn(Schedulers.computation())
                .subscribe {
                    stopSelf()
                }
        composites += ShellSU.commonSU.rxRootEnable
                .filter { it == true }
                .observeOn(Schedulers.computation())
                .subscribe {
                    gestureActions?.onDetect()
                    gestureDetector?.enable(true)
                }

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

    private fun closeService()
    {
        if (BuildConfig.DEBUG){
            Log.d("Stop service", "**************************")
        }

        composites.clear()

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(onEventIntent)
        }catch (e:Exception){}
        try {
            unregisterReceiver(onEventIntent)
        }catch (e:Exception){}

        gestureDetector?.hw?.screenLock()
        gestureDetector?.close()
        gestureActions?.close()

        su.close()
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
    private val onEventIntent = object : BroadcastReceiver()
    {
        //  Events for screen on and screen off
        override fun onReceive(context: Context, intent: Intent)
        {
            when (intent.action) {
                //  Screen OFF
                Intent.ACTION_SCREEN_OFF -> {
                    setServiceForeground(true)

                    gestureDetector?.screenOnMode = false
                    gestureActions?.screenOnMode = false
                    gestureDetector?.hw?.screenLock()
                }
                //  Screen ON
                Intent.ACTION_SCREEN_ON -> {
                    setServiceForeground(false)
                    gestureDetector?.screenOnMode = true
                    gestureActions?.screenOnMode = true
                }
/*
                //  Enable gestures
                GestureSettings.EVENT_ENABLE ->{
                    val key = intent.getSerializableExtra("key") as String?
                    if (key != GestureSettings.GESTURE_ENABLE) return

                    val bEnable = intent.getSerializableExtra("value") as Boolean? == true

                    if (bEnable) {
                        gestureActions?.onDetect()
                        gestureDetector?.enable(true)
                    } else {
                        stopSelf()
                    }

                }
*/
            }
        }
    }
}
