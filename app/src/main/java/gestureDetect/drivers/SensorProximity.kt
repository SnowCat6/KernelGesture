package gestureDetect.drivers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import gestureDetect.GestureDetect
import gestureDetect.tools.RxScreenOn
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import ru.vpro.kernelgesture.BuildConfig

/**
 * Класс для определение жеста сенсором приближения
 */

open class SensorProximity(gesture: GestureDetect) :
        SensorHandler(gesture), SensorEventListener
{
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null
    private var bRegisterEvent = false

    private var bNearSensor = false

    private var longTimeFar = System.currentTimeMillis()
    private var nearTimeNear = System.currentTimeMillis()
    private var bLongTrigger = false

    private val sensor1wait = 1*1000
    private val sensor2wait = 1*1000

    private var rxScreen : RxScreenOn? = null

    private val composites = CompositeDisposable()

    override fun onCreate(context: Context)
    {
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (mProximity == null) return

        gesture.addSupport(listOf("PROXIMITY", "KEY_PROXIMITY"))
        gesture.registerScreenEvents("KEY_PROXIMITY", "KEY_PROXIMITY_ON")

        if (composites.size() != 0) return

        rxScreen = rxScreen ?: RxScreenOn(context.applicationContext)
        composites += rxScreen!!.subscribe { onResume() }
    }

    override fun close()
    {
        composites.clear()
        super.close()
    }

    override fun onResume()
    {
        if (rxScreen?.isScreenOn() == true)
        {
            if (!gesture.settings.getEnable("KEY_PROXIMITY_ON")) {
                onPause()
                return
            }
        }else{
            if (!gesture.settings.getEnable("KEY_PROXIMITY")) {
                onPause()
                return
            }
        }

        if (bRegisterEvent) return
        bRegisterEvent = true

        if (BuildConfig.DEBUG){
            Log.d("Proximity", "start")
        }

        longTimeFar = System.currentTimeMillis()
        mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        if (!bRegisterEvent) return
        bRegisterEvent = false

        if (BuildConfig.DEBUG){
            Log.d("Proximity", "stop")
        }

        mSensorManager?.unregisterListener(this, mProximity)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    override fun onSensorChanged(event: SensorEvent)
    {
        // If registered use proximity - change value detector
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return

        val bNearSensorNow = event.values[0].toInt() == 0
        //  Filter sensor state
        if (bNearSensor ==bNearSensorNow) return
        bNearSensor = bNearSensorNow

        if (BuildConfig.DEBUG) {
            Log.d("Near", bNearSensorNow.toString())
        }

        if (bNearSensor){
            //  Start near timer
            nearTimeNear = System.currentTimeMillis()
            //  Check pre event timeout
            bLongTrigger = (System.currentTimeMillis() - longTimeFar) > sensor1wait
        }else{
            //  Start far timer
            longTimeFar = Math.max(System.currentTimeMillis(), longTimeFar)
            //  If pre event timer not true do not fire event
            if (!bLongTrigger) return

            if (BuildConfig.DEBUG) {
                Log.d("Near pulse time", (System.currentTimeMillis() - nearTimeNear).toString())
            }

            //  Check pulse time of near event
            val bNearTrigger = (System.currentTimeMillis() - nearTimeNear) < sensor2wait
            if (!bNearTrigger) return

            //  Fire event
            sensorEvent("KEY_PROXIMITY")
        }
    }
}