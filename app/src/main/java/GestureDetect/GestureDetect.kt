package GestureDetect

import SuperSU.ShellSU
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import GestureDetect.drivers.sensor.SensorHandler
import GestureDetect.drivers.sensor.SensorInput
import GestureDetect.drivers.sensor.SensorProximity
import ru.vpro.kernelgesture.BuildConfig
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class GestureDetect (val context:Context)
{
    /**
     * Sensor devices
     */
    private val sensorHandlers = arrayOf(
            SensorProximity(this),
            SensorInput(this)
    )

    /**
     * Devices control
     */
    //  Supported devices and keys
    private var supported = emptyList<String>()
    //  Detected sensor devices
    private var sensorDevices = emptyList<SensorHandler>()
    //  SuperSU
    val su = ShellSU()
    /**
     * Disable event detection
     */
    private var _disabled = false
    var disabled: Boolean
        get() = _disabled
        set(value) {
            _disabled = value
            if (value) eventMutex.unlock()
        }

    /**
     * Events with wait 500ms for wtice
     */
    private var delayEvents = emptyList<Pair<String, String>>()
    fun registerDelayEvents(event:String, delayEvent:String)
    {
        delayEvents
                .filter { (first) -> first.contains(event) }
                .forEach { return }
        delayEvents += Pair(event, delayEvent)
    }
    /**
     *  Settings for filter events with proximity sensor
     */
    private var timeNearChange = GregorianCalendar.getInstance().timeInMillis
    private var _bIsNearProximity:Boolean = false
    var isNearProximity: Boolean
        get() {
            val timeDiff = GregorianCalendar.getInstance().timeInMillis - timeNearChange
            return _bIsNearProximity || timeDiff < 1*1000
        }
        set(value) {
            if (_bIsNearProximity != value) {
                _bIsNearProximity = value
                timeNearChange = GregorianCalendar.getInstance().timeInMillis
            }
        }

    val eventMutex = Mutex()
    /**
     * CODE
     */
    init{
        detectDevices()
    }

    fun close()
    {
        disabled = true
        eventMutex.unlock()
        sensorDevices.forEach{ it.close() }
        delayEvents = emptyList()
    }

    fun enable(bEnable: Boolean)
    {
        detectDevices()
        sensorDevices.forEach { it.enable(bEnable) }
    }

    private fun detectDevices()
    {
        eventMutex.unlock()
        val prevSensor = sensorDevices

        delayEvents = emptyList()
        supported = emptyList()

        sensorDevices = sensorHandlers.filter { it.onDetect() }
        prevSensor
                .filter { !sensorDevices.contains(it) }
                .forEach { it.close() }
    }

    private  var sensorEventGesture:String? = null
    fun waitGesture(): String?
            = getEventThread()

    private fun getEventThread():String?
    {
        if (!su.hasRootProcess() && su.checkRootAccess())
        {
            detectDevices()
            if (disabled) return null
        }

        sensorDevices.forEach { it.onStart() }

        var thisEvent:String?
        do{
            thisEvent = null
            sensorEventGesture = null
            eventMutex.lock() // Wait unlock
            if (disabled) break

            thisEvent = sensorEventGesture
            thisEvent?.apply {

                if (BuildConfig.DEBUG) {
                    Log.d("Lock gesture", thisEvent)
                }

                for ((first, second) in delayEvents) {

                    if (first != thisEvent) continue
                    if (!getEnable(context, second)) break
                    if (eventMutex.lock(500)) thisEvent = second
                    break
                }
            }
        }while (!disabled && !getEnable(context, thisEvent))

        sensorDevices.forEach { it.onStop() }

        return thisEvent
    }

    fun sensorEvent(value:String):Boolean
    {
        if (disabled) return false
//        if (!getEnable(context, value)) return false

        sensorEventGesture = value
        eventMutex.unlock()

        return true
    }

    fun addSupport(value:String)
    {
        if (supported.contains(value)) return
        supported += value
    }
    fun addSupport(value:Array<String>)
            = value.forEach { addSupport(it) }

    fun getSupport():List<String>
    {
/*
        if (SU.exec("find /sys -name *gesture*") && SU.exec("echo --END--"))
        {
            while (true) {
                val line = SU.readExecLine() ?: break
                Log.d("Read line", line)
                if (line == "--END--") break

                val path =  line.substring(line.lastIndexOf("/")+1)
                when(path){
//                        "tpgesture_status" -> { addSupport("GESTURE") }
                }
            }
        }
 */
        return supported
    }

    companion object
    {
        fun getAllEnable(context: Context): Boolean
                = getEnable(context, "GESTURE_ENABLE")

        fun setAllEnable(context: Context, value: Boolean) {
            setEnable(context, "GESTURE_ENABLE", value)

            val gs = GestureDetect(context)
            gs.enable(value)
        }
        fun getEnable(context: Context, key: String?): Boolean
        {
            if (key == null) return false
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getBoolean(key, false)
        }
        fun setEnable(context: Context, key: String, value: Boolean)
        {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val e = sharedPreferences.edit()
            e.putBoolean(key, value)
            e.apply()
        }

        fun getAction(context: Context, key: String): String? {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            try {
                return sharedPreferences.getString("${key}_ACTION", null)
            } catch (e: Exception) { }
            return null
        }

        fun setAction(context: Context, key: String, value: String?)
        {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val e = sharedPreferences.edit()
            if (value == null){
                e.remove(key)
            }else {
                e.putString("${key}_ACTION", value)
            }
            e.apply()
        }
    }
    class Mutex
    {
        private var bLocked = false
        private var semaphore = Semaphore(0)

        fun lock()
        {
            if (bLocked) return
            bLocked = true

            if (BuildConfig.DEBUG) {
                Log.d("Lock", "Wait semaphore lock")
            }

            semaphore.acquire()
            bLocked = false
        }
        fun lock(timeout:Long):Boolean
        {
            if (bLocked) return false
            bLocked = true

            if (BuildConfig.DEBUG) {
                Log.d("Lock", "Wait ${timeout}ms semaphore lock")
            }

            val bRet = semaphore.tryAcquire(1, timeout, TimeUnit.MILLISECONDS)

            bLocked = false
            return bRet
        }
        fun unlock()
        {
            if (!bLocked) return
            bLocked = false

            if (BuildConfig.DEBUG) {
                Log.d("Lock", "Unlock locked semaphore")
            }
            semaphore.release()
        }
    }
}
