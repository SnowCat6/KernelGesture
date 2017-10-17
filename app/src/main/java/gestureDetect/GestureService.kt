package gestureDetect

import SuperSU.ShellSU
import android.app.IntentService
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
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

    companion object {
        var bDisableService = false
    }
    /************************************/
    /*
    GESTURE DETECT
     */
    override fun onHandleIntent(intent: Intent?)
    {
        val settings = GestureSettings(this)
        val hw = GestureHW(this)
        hw.registerEvents()
        su.checkRootAccess()

        val gesture = gestureDetector ?: GestureDetect(this, su)
        gestureDetector = gesture

        val actions = gestureActions ?: GestureAction(this, su)
        gestureActions = actions

        gesture.onCreate(this)
        //  Enable/disable gestures on start service
        gesture.enable(true)
        actions.onCreate()

        setServiceForeground(!hw.isScreenOn())

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
            if (bDisableService) continue
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
        hw.unregisterEvents()
        actions.onStop()
        gesture.close()

        setServiceForeground(false)
    }
    private fun setServiceForeground(bSetForeground:Boolean)
    {
        if (bForeground == bSetForeground) return
        bForeground = bSetForeground

        if (BuildConfig.DEBUG){
            Log.d("Service foreground", bForeground.toString())
        }

        if (bForeground) {
            val builder = Notification.Builder(this )
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

        //  Get sensor devices
        mSensorManager  = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity      = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        composites += GestureSettings.rxUpdateValue
                .filter { it.key ==  GestureSettings.GESTURE_ENABLE && it.value == false }
                .observeOn(Schedulers.io())
                .subscribe {
                    stopSelf()
                }

        composites += GestureHW.rxScreenOn.subscribe {

            setServiceForeground(!it)
            if (!it)gestureDetector?.hw?.screenLock()
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

        gestureDetector?.hw?.screenLock()
        gestureDetector?.hw?.unregisterEvents()
        gestureDetector?.close()
        gestureActions?.close()
//        su.killJobs()
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
}
