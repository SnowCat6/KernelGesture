package gesture

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import gesture.drivers.sensor.SensorHandler
import gesture.drivers.sensor.SensorInput
import gesture.drivers.sensor.SensorProximity
import ru.vpro.kernelgesture.BuildConfig
import java.io.BufferedReader
import java.io.OutputStream
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
    private var supported = emptyArray<String>()
    //  Detected sensor devices
    private var sensorDevices = emptyArray<SensorHandler>()
    //  Get events in process
    private var bGetEvents = false
    /**
     * Internal variables
     */
    private var _bLock = false
    var lock: Boolean
        get() = _bLock
        set(value) {
            _bLock = value
            if (_bLock) eventMutex.unlock()
        }

    var delayEvents = emptyArray<Pair<String, String>>()
    fun registerDelayEvents(event:String, delayEvent:String)
    {
        delayEvents
                .filter { (first) -> first.contains(event) }
                .forEach { return }
        delayEvents += Pair(event, delayEvent)
    }
    /**
     *
     */
    private var timeNearChange = GregorianCalendar.getInstance().timeInMillis
    private var _bIsNear:Boolean = false
    var isNear: Boolean
        get() {
            val timeDiff = GregorianCalendar.getInstance().timeInMillis - timeNearChange
            return _bIsNear || timeDiff < 1*1000
        }
        set(value) {
            if (_bIsNear != value) {
                _bIsNear = value
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
        lock = true
        SU.close()
    }

    fun enable(powerOn: Boolean)
    {
        sensorDevices.forEach {
            it.enable(powerOn)
        }
    }

    private fun detectDevices()
    {
        eventMutex.unlock()

        sensorDevices = emptyArray()
        sensorHandlers.forEach {
            if (it.onDetect()) sensorDevices += it
        }
    }

    var sensorEventGesture:String? = null
    fun waitGesture(): String?
            = getEventThread()

    private fun getEventThread():String?
    {
        if (!SU.hasRootProcess() && SU.checkRootAccess())
        {
            detectDevices()
            if (lock) return null
        }

        sensorEventGesture = null
        sensorDevices.forEach { it.onStart() }

        eventMutex.lock() // Wait unlock
        var thisEvent = sensorEventGesture

        if (thisEvent != null)
        {
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

        sensorDevices.forEach { it.onStop() }

        return thisEvent
    }

    fun sensorEvent(value:String):Boolean
    {
        if (lock) return false
        if (!getEnable(context, value)) return false

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

    fun getSupport():Array<String>
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
        fun getEnable(context: Context, key: String): Boolean {
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
    /**
     * SuperSU wrapper
     */
    object SU
    {
        private var processSU: Process? = null
        private var errorSU: BufferedReader? = null
        private var readerSU: BufferedReader? = null
        private var writerSU: OutputStream? = null
        private var bEnableSU = false
        private var bEnableCheck = true

        val EVENT_UPDATE = "UPDATE-ROOT"

        fun checkRootAccess():Boolean
                = SU.open() != null

        fun hasRootProcess():Boolean
                = bEnableSU

        fun enable(bEnable:Boolean) {
            bEnableCheck = bEnable
        }

        fun open(): Process?
        {
            if (!bEnableCheck)
                return processSU


            try{
                val exit = processSU?.exitValue()
                if (exit != null) close()
            }catch (e:Exception){}

            if (processSU != null)
                return processSU

            synchronized(bEnableSU)
            {
                try {
                    processSU = Runtime.getRuntime().exec("su")
                    readerSU = processSU?.inputStream?.bufferedReader()
                    errorSU = processSU?.errorStream?.bufferedReader()
                    writerSU = processSU?.outputStream

                    exec("id")
                    val id = readExecLine()?.contains("root")
                    if (id == null || id == false) close()

                } catch (e: Exception) {
                    e.printStackTrace()
                    close()
                }
                bEnableSU = processSU != null
                bEnableCheck = bEnableSU
            }
            return processSU
        }

        fun exec(cmd: String): Boolean
        {
            SU.open()

            try {
                writerSU?.apply {
                    write("$cmd\n".toByteArray())
                    flush()
                }
            }catch (e:Exception){
                e.printStackTrace()
                return false
            }

            if (BuildConfig.DEBUG) {
                Log.d("GestureDetect exec", "$cmd\n")
            }

            return true
        }

        fun close()
        {
            if (processSU == null) return
            try {
                processSU?.destroy()
            }catch (e:Exception){
                e.printStackTrace()
            }
            processSU = null
            writerSU = null
            readerSU = null
            errorSU = null

            bEnableSU = false
        }

        fun getFileLine(name: String): String?
        {
            if (exec("echo $(cat $name)")) return readExecLine()
            return null
        }

        fun isFileExists(name: String): Boolean
        {
            if (exec("[ -f $name ] && echo 1 || echo 0")) {
                return readExecLine() == "1"
            }
            return false
        }

        fun readExecLine(): String?
        {
            try {
                return  readerSU?.readLine()
            }catch (e:Exception){
                e.printStackTrace()
            }
            return null
        }

        fun readErrorLine() : String? {
            try {
                return  errorSU?.readLine()
            }catch (e:Exception){
                e.printStackTrace()
            }
            return null
        }
    }
}
