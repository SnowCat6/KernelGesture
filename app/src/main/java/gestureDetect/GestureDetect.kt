package gestureDetect

import SuperSU.ShellSU
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import gestureDetect.drivers.sensor.SensorHandler
import gestureDetect.drivers.sensor.SensorInput
import gestureDetect.drivers.sensor.SensorProximity
import ru.vpro.kernelgesture.BuildConfig
import java.io.Serializable
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

    private var _screenOnMode:Boolean = false
    var screenOnMode:Boolean
        get () = _screenOnMode
        set(value){
            if (value != _screenOnMode){
                if (bWaitEvent) onStop()
                _screenOnMode = value
                if (bWaitEvent) onStart()
            }
        }

    private var screenEvents = emptyList<Pair<String, String>>()
    fun registerScreenEvents(event:String, screenEvent:String)
    {
        screenEvents
                .filter { (first) -> first.contains(event) }
                .forEach { return }

        screenEvents += Pair(event, screenEvent)
        addSupport(event)
        addSupport(screenEvent)
    }
    /**
     * Events with wait 500ms for twice
     */
    private var delayEvents = emptyList<Pair<String, String>>()
    fun registerDelayEvents(event:String, delayEvent:String)
    {
        delayEvents
                .filter { (first) -> first.contains(event) }
                .forEach { return }
        delayEvents += Pair(event, delayEvent)
        addSupport(event)
        addSupport(delayEvent)
    }
    /**
     *  Settings for filter events with proximity sensor
     */
    private var timeNearChange = System.currentTimeMillis()
    private var _bIsNearProximity:Boolean = false
    var isNearProximity: Boolean
        get() {
            val timeDiff = System.currentTimeMillis() - timeNearChange
            return _bIsNearProximity || timeDiff < 1*1000
        }
        set(value) {
            if (_bIsNearProximity != value) {
                _bIsNearProximity = value
                timeNearChange = System.currentTimeMillis()
            }
        }

    private val eventMutex = Mutex()
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
        onStop()
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
        if (bWaitEvent) onStop()

        val prevSensor = sensorDevices
        delayEvents = emptyList()
        supported = emptyList()

        sensorDevices = sensorHandlers.filter { it.onDetect() }
        prevSensor.subtract(sensorDevices).forEach { it.close() }

        if (bWaitEvent) onStart()
    }

    private var bStart = false
    private fun onStart(){
        if (bStart) return
        bStart = true
        if (BuildConfig.DEBUG){
            Log.d("GestureDetect", "start")
        }
        sensorDevices.forEach { it.onStart() }
    }
    private fun onStop(){
        if (!bStart) return
        bStart = false
        if (BuildConfig.DEBUG){
            Log.d("GestureDetect", "stop")
        }
        sensorDevices.forEach { it.onStop() }
    }

    private  var sensorEventGesture = LinkedList<String>()
    fun waitGesture(): String?
            = getEventThread()

    var bWaitEvent = false
    private fun getEventThread():String?
    {
        if (!su.hasRootProcess() && su.checkRootAccess()) {
            detectDevices()
            if (disabled) return null
        }

        bWaitEvent = true
        sensorEventGesture.clear()
        onStart()

        var thisEvent:String?
        do{
            thisEvent = getCurrentEvent()
            if (disabled) break

            thisEvent?.apply {

                if (BuildConfig.DEBUG) {
                    Log.d("Lock gesture", thisEvent)
                }

                delayEvents.firstOrNull {
                    val evON = if (screenOnMode) screenEvents.firstOrNull { (first, second) -> it.second == first }?.second else null
                    it.first == thisEvent && (getEnable(context, it.second) || getEnable(context, evON))
                }?.apply {
                    val evDelay = getCurrentEvent(350)
                    if (evDelay == first) thisEvent = second
                }
                if (screenOnMode){
                    thisEvent = screenEvents.firstOrNull {
                        it.first == thisEvent && getEnable(context, it.second)
                    }?.second
                }
            }
        }while (!disabled && !getEnable(context, thisEvent))

        onStop()
        bWaitEvent = false

        return thisEvent
    }
    private fun getCurrentEvent():String?
    {
        with(sensorEventGesture) {
            synchronized(sensorEventGesture) {
                if (isNotEmpty()) return pop()
            }
            eventMutex.lock()
            synchronized(sensorEventGesture) {
                return if (isNotEmpty()) pop() else null
            }
        }
    }
    private fun getCurrentEvent(timeout:Long):String?
    {
        with(sensorEventGesture) {

            synchronized(sensorEventGesture) {
                if (isNotEmpty()) return pop()
            }
//            eventMutex.lock(timeout)
            Thread.sleep(timeout)
            synchronized(sensorEventGesture) {
                return if (isNotEmpty()) pop() else null
            }
        }
    }

    fun sensorEvent(value:String):Boolean
    {
        if (disabled) return false

        if (BuildConfig.DEBUG){
            Log.d("SensorEvent", value)
        }
        GestureAction.HW.powerON(context)
        synchronized(sensorEventGesture){
            sensorEventGesture.push(value)
            eventMutex.unlock()
        }

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
        val EVENT_ENABLE = "EVENT_ENABLE"

        fun getAllEnable(context: Context): Boolean
                = getEnable(context, "GESTURE_ENABLE")

        fun setAllEnable(context: Context, value: Boolean) {
            setEnable(context, "GESTURE_ENABLE", value)
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

            val intent = Intent(GestureDetect.EVENT_ENABLE)
            intent.putExtra("key", key as Serializable)
            intent.putExtra("value", value as Serializable)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
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

        fun lock() {
            synchronized(bLocked) {
                if (bLocked) return
                bLocked = true
             }

            if (BuildConfig.DEBUG) {
                Log.d("Lock", "Wait semaphore lock")
            }
            semaphore.acquire()
            synchronized(bLocked) {
                bLocked = false
            }
        }
        fun lock(timeout:Long):Boolean
        {
            synchronized(bLocked)
            {
                if (bLocked) return false
                bLocked = true
                if (BuildConfig.DEBUG) {
                    Log.d("Lock", "Wait ${timeout}ms semaphore lock")
                }
            }

            val bRet = semaphore.tryAcquire(1, timeout, TimeUnit.MILLISECONDS)

            synchronized(bLocked) {
                bLocked = false
            }
            return bRet
        }
        fun unlock()
        {
            synchronized(bLocked)
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
}
