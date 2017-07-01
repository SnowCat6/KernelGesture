package gesture

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.view.Display
import android.widget.Toast
import gesture.drivers.sensor.SensorHandler
import gesture.drivers.sensor.SensorInput
import gesture.drivers.sensor.SensorProximity
import ru.vpro.kernelgesture.BuildConfig
import java.io.BufferedReader
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Semaphore


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
            if (_bLock) closeEvents()
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
    /**
     * CODE
     */
    init{
        detectDevices()
    }

    fun close()
    {
        lock = true
        sensorDevices.forEach { it.close() }
        closeEvents()
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
        closeEvents()
        sensorDevices.forEach { it.close() }

        sensorDevices = emptyArray()
        sensorHandlers.forEach {
            if (it.onDetect()) sensorDevices += it
        }
    }

    var semaphore = Semaphore(0)
    var sensorEventGesture:String? = null

    fun waitGesture(): String?
    {
        return getEventThread()
    }

    private fun getEventThread():String?
    {
        if (!SU.hasRootProcess() && SU.checkRootAccess())
        {
            detectDevices()
            if (lock) return null
        }

        sensorEventGesture = null
        bGetEvents = true
        sensorDevices.forEach { it.onStart() }

        if (BuildConfig.DEBUG) {
            Log.d("Lock", "Wait semaphore lock")
        }

        semaphore.acquire() // Wait unlock
        var thisEvent = sensorEventGesture
        sensorEventGesture = null

        if (thisEvent != null) {
            for ((first, second) in delayEvents) {

                if (first != thisEvent) continue
                if (!getEnable(context, second)) break

                Thread.sleep(500)
                if (sensorEventGesture != null && sensorEventGesture == thisEvent)
                    thisEvent = second
                break
            }
        }

        closeEvents()

        if (BuildConfig.DEBUG && thisEvent != null) {
            Log.d("Lock gesture", thisEvent)
        }

        return thisEvent
    }

    private fun closeEvents()
    {
        if (!bGetEvents) return
        bGetEvents = false

        if (BuildConfig.DEBUG) {
            Log.d("Lock", "Unlock locked semaphore")
        }

        semaphore.release()

        sensorDevices.forEach { it.onStop() }
    }
    fun sensorEvent(value:String):Boolean
    {
        if (lock) return false
        if (!getEnable(context, value)) return false

        sensorEventGesture = value
        closeEvents()

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
