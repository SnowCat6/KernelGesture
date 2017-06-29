package gesture.drivers.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import gesture.GestureDetect
import ru.vpro.kernelgesture.BuildConfig
import java.util.*

/**
 * Класс для определение жеста сенсором приближения
 */

open class SensorProximity(gesture: GestureDetect) :
        SensorHandler(gesture), SensorEventListener
{
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null
    private var bRegisterEvent = false

    var bNearSensor = false

    var longTimeFar = GregorianCalendar.getInstance().timeInMillis
    var nearTimeNear = GregorianCalendar.getInstance().timeInMillis
    var bLongTrigger = false

    val sensor1wait = 1*1000
    val sensor2wait = 1*1000

    override fun onDetect(): Boolean
    {
        mSensorManager = gesture.context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (mProximity == null) return false

        gesture.addSupport(arrayOf("PROXIMITY", "KEY_PROXIMITY"))
        return true
    }

    override fun onStart()
    {
        if (!GestureDetect.getEnable(gesture.context, "KEY_PROXIMITY"))
            return

        bRegisterEvent = true
        longTimeFar = GregorianCalendar.getInstance().timeInMillis// + sensor1wait
        mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStop() {
        if (!bRegisterEvent) return
        bRegisterEvent = false
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
            nearTimeNear = GregorianCalendar.getInstance().timeInMillis
            //  Check pre event timeout
            bLongTrigger = (GregorianCalendar.getInstance().timeInMillis - longTimeFar) > sensor1wait
        }else{
            //  Start far timer
            longTimeFar = Math.max(GregorianCalendar.getInstance().timeInMillis, longTimeFar)
            //  If pre event timer not true do not fire event
            if (!bLongTrigger) return

            if (BuildConfig.DEBUG) {
                Log.d("Near pulse time", (GregorianCalendar.getInstance().timeInMillis - nearTimeNear).toString())
            }

            //  Check pulse time of near event
            val bNearTrigger = (GregorianCalendar.getInstance().timeInMillis - nearTimeNear) < sensor2wait
            if (!bNearTrigger) return

            //  Fire event
            sensorEvent("KEY_PROXIMITY")
        }
    }
}