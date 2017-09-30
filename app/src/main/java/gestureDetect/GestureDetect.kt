package gestureDetect

import SuperSU.ShellSU
import android.content.Context
import android.util.Log
import gestureDetect.drivers.SensorHandler
import gestureDetect.drivers.SensorInput
import gestureDetect.drivers.SensorProximity
import gestureDetect.tools.GestureHW
import gestureDetect.tools.GestureSettings
import io.reactivex.subjects.BehaviorSubject
import ru.vpro.kernelgesture.BuildConfig
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class GestureDetect (val context : Context,
                     val su : ShellSU = ShellSU())
{
    /**
     * Sensor devices
     */
    private val sensorHandlers = arrayOf(
            SensorProximity(this),
            SensorInput(this)
    )

    val hw      = GestureHW(context)
    val settings= GestureSettings(context)
    /**
     * Devices control
     */
    //  Supported devices and keys
    private var supported = emptyList<String>()
    /**
     * Disable event detection
     */
    var bClosed: Boolean = false
        set(value) {
            field = value
            if (value) eventMutex.unlock()
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
    var isNearProximity: Boolean = false
        get() {
            val timeDiff = System.currentTimeMillis() - timeNearChange
            return field || timeDiff < 1*1000
        }
        set(value) {
            if (field == value) return
            field = value
            timeNearChange = System.currentTimeMillis()
        }

    private val eventMutex = Mutex()

    fun close()
    {
        bClosed = true

        onPause()
        sensorHandlers.forEach{ it.close() }

        eventMutex.unlock()
    }

    fun enable(bEnable: Boolean)
    {
        sensorHandlers.forEach { it.enable(bEnable) }
    }

    fun onCreate()
    {
        bLockSupportUpdate = true
        sensorHandlers.forEach { it.onCreate() }
        bLockSupportUpdate = false
        rxSupportUpdate.onNext(supported)
    }

    private var bStart = false
    private fun onResume(){
        if (bStart) return
        bStart = true

        if (BuildConfig.DEBUG){
            Log.d("GestureDetect", "start")
        }
        sensorHandlers.forEach { it.onResume() }
    }
    private fun onPause(){
        if (!bStart) return
        bStart = false

        if (BuildConfig.DEBUG){
            Log.d("GestureDetect", "stop")
        }
        sensorHandlers.forEach { it.onPause() }
    }

    private  var sensorEventGesture = LinkedList<String>()
    fun waitGesture(): String?
            = getEventThread()

    private fun getEventThread():String?
    {
        sensorEventGesture.clear()
        onResume()

        var thisEvent:String?
        do{
            thisEvent = getCurrentEvent()
            if (bClosed) break

            thisEvent?.apply {

                if (BuildConfig.DEBUG) {
                    Log.d("Lock gesture", thisEvent)
                }

                if (GestureHW.screenON)
                {
                    delayEvents.find { it.first == thisEvent  }?.apply {
                        screenEvents.find { it.first == second && settings.getEnable(second) }?.apply {
                            val evDelay = getCurrentEvent(350)
                            if (evDelay == thisEvent) thisEvent = first
                        }
                    }
                }else{
                    delayEvents.find { it.first == thisEvent && settings.getEnable(it.second) }?.apply {
                        val evDelay = getCurrentEvent(350)
                        if (evDelay == first) thisEvent = second
                    }
                }

                if (GestureHW.screenON){
                    thisEvent = screenEvents.find {
                        it.first == thisEvent && settings.getEnable(it.second)
                    }?.second
                }
            }
        }while (!bClosed && !settings.getEnable(thisEvent))

        onPause()

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
            eventMutex.lock(timeout)
//            Thread.sleep(timeout)
            synchronized(sensorEventGesture) {
                return if (isNotEmpty()) pop() else null
            }
        }
    }

    fun sensorEvent(value:String):Boolean
    {
        if (bClosed) return false
        if (!bStart) return false
        if (!isEventEnable(value)) return false

        if (BuildConfig.DEBUG){
            Log.d("SensorEvent", value)
        }
        hw.powerON()
        synchronized(sensorEventGesture){
            sensorEventGesture.push(value)
            eventMutex.unlock()
        }

        return true
    }

    /**
     * Once event or double tap events enable
     */
    private fun isEventEnable(event:String):Boolean
    {
        if (GestureHW.screenON){
            var ev = event
            delayEvents.find { it.first == ev}?.second?.apply { ev = this }
            return screenEvents.find { it.first == ev && settings.getEnable(it.second) } != null
        }

        if (settings.getEnable(event)) return true
        return delayEvents.find { it.first == event && settings.getEnable(it.second) } != null
    }

    private var bLockSupportUpdate = false
    val rxSupportUpdate = BehaviorSubject.createDefault(supported)
    fun addSupport(value:String)
    {
        if (supported.contains(value)) return
        supported += value
        if (!bLockSupportUpdate) rxSupportUpdate.onNext(supported)
    }
    fun addSupport(value:List<String>)
            = value.forEach { addSupport(it) }

    fun getSupport():List<String>
            = supported

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
