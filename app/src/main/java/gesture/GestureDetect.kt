package gesture

import java.io.*
import android.content.*
import android.os.PowerManager
import android.preference.PreferenceManager
import android.util.Log
import android.os.Vibrator
import ru.vpro.kernelgesture.BuildConfig
import android.view.Display
import android.hardware.display.DisplayManager
import android.os.Handler
import android.widget.Toast
import android.os.Looper
import gesture.drivers.input.*
import gesture.drivers.sensor.SensorHandler
import gesture.drivers.sensor.SensorProximity
import java.util.*


class GestureDetect (val context:Context)
{
    /**
     * Input devices
     */
    private val inputHandlers = arrayOf(
            InputMTK_TPD(this), InputMTK_KPD(this),
            InputQCOMM_KPD(this), InputFT5x06_ts(this),
            InputSunXi_KPD(this)
    )

    /**
     * Sensor devices
     */
    private val sensorHandlers = arrayOf(
            SensorProximity(this)
    )

    /**
     * Devices control
     */
    //  Supported devices and keys
    private var supported = emptyArray<String>()
    //  Detected input and sensor devices
    private var inputDevices = emptyArray<Pair<String, InputHandler>>()
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

    fun close(){
        lock = true
        closeEvents()
        SU.close()
    }

    fun enable(powerOn: Boolean)
    {
        if (SU.hasRootProcess()) {
            inputDevices.forEach { it.second.setEnable(powerOn) }
        }
    }

    private fun detectDevices()
    {
        closeEvents()
        inputDevices = emptyArray()
        sensorDevices = emptyArray()

        getInputDevices().forEach { (input, name) ->
            inputHandlers.forEach {
                if (it.onDetect(name)) inputDevices += Pair(input, it)
            }
        }

        sensorHandlers.forEach {
            if (it.onDetect()) sensorDevices += it
        }
    }

    private fun getInputDevices():Array<Pair<String, String>>
    {
        var inputs = emptyArray<Pair<String, String>>()
        try {
            var name = ""
            BufferedReader(FileReader("/proc/bus/input/devices"))
                    .readLines()
                    .forEach { line ->

                        val iName = line.indexOf("Name=")
                        if (iName >= 0) name = line.substring(iName + 5).trim('"')

                        val iHandlers = line.indexOf("Handlers=")
                        if (iHandlers >= 0)
                        {
                            line.substring(iHandlers + 9)
                                .split(" ")
                                .filter { it.contains("event") }
                                .forEach { inputs += Pair("/dev/input/$it", name) }
                        }
                    }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inputs
    }

    fun waitGesture(): String?
    {
        if (SU.open() == null) return null
        return getEvent()
    }

    private fun getEvent(): String?
    {
        var device = ""
        var bQueryFound = false

        ++queryIx
        bGetEvents = true

        sensorDevices.forEach { it.onStart() }

        //  For each device
        var ix = 0
        inputDevices.forEach { (inputName, device) ->
            //  Power on gesture if available, many drivers not set this value if screen off
            device.setEnable(true)
            //  Run input event detector
            ++ix
            SU.exec("while v$ix=$(getevent -c 2 -l $inputName)  ; do for i in 1 2 3 4 ; do echo $inputName\\\\n\"\$v$ix\">&2 ; done ; done &")
           ++ix
            SU.exec("while v$ix=$(getevent -c 4 -l $inputName)  ; do for i in 1 2 ; do echo $inputName\\\\n\"\$v$ix\">&2 ; done ; done &")
        }

        SU.exec("echo query$queryIx>&2")
        while(!lock && bGetEvents)
        {
            //  Read line from input
            val line = SU.readErrorLine() ?: break

            //  Stop if gesture need stop run
            if (lock || !bGetEvents) break

            //  Check query number for prevent old events output
            if (!bQueryFound){
                bQueryFound = line == "query$queryIx"
                continue
            }
            if (line.contains("CLOSE_EVENTS"))
                break

            if (line.contains("SENSOR_EVENT")){
                closeEvents()
                return line.split(" ")[1]
            }

            //  Detect current input device
            if (line.contains("/dev/input/")) {
                device = line
                continue
            }

            //  Check only key events
            if (!line.contains("EV_")) continue

            //  Check device is near screen
            if (isNear) continue

            //  Find device for event accept
            for ((first, second) in inputDevices)
            {
                if (first != device) continue

                if (BuildConfig.DEBUG) {
                    Log.d("Event detected", line)
                }
                //  Get gesture
                val gesture = second.onEvent(line) ?: break
                //  Check gesture enable
                if (!getEnable(context, gesture)) break
                //  Close cmd events
                closeEvents()
                //  Return result
                return gesture
            }
        }

        closeEvents()

        //  Error reading, no continue or sensorEvent handled
        return null
    }
    private fun closeEvents()
    {
        if (!bGetEvents) return
        bGetEvents = false

        SU.exec("kill -s SIGINT %%")
        sensorDevices.forEach { it.onStop() }

        //  Many execute for flush process buffer
        for(ix in 0..15) SU.exec("echo CLOSE_EVENTS>&2")
    }
    fun sensorEvent(value:String):Boolean
    {
        if (lock) return false
        if (!getEnable(context, value)) return false

        //  Many execute for flush process buffer
        for(ix in 0..15) SU.exec("echo SENSOR_EVENT $value>&2")

        return true
    }

    fun addSupport(value:String)
    {
        if (supported.contains(value)) return
        supported += value
    }
    fun addSupport(value:Array<String>)
            = value.forEach { addSupport(it) }

    private fun toast(value:String)
    {
        screenON(context)
        vibrate(context)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, value, Toast.LENGTH_LONG).show()
        }
    }
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
        private var queryIx = 0

        fun getAllEnable(context: Context): Boolean
        {
            return getEnable(context, "GESTURE_ENABLE")
        }
        fun setAllEnable(context: Context, value: Boolean) {
            setEnable(context, "GESTURE_ENABLE", value)

            val gs = GestureDetect(context)
            gs.enable(value)
        }
        fun getEnable(context: Context, key: String): Boolean {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getBoolean(key, false)
        }
        fun setEnable(context: Context, key: String, value: Boolean) {
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

        /*
            <uses-permission android:name="android.permission.WAKE_LOCK" />
        */
        fun screenON(context:Context)
        {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG")
            wakeLock.acquire(500)
//            wakeLock.release()
        }
        /*
        <uses-permission android:name="android.permission.VIBRATE"/>
         */
        fun vibrate(context:Context){
            // Vibrate for 500 milliseconds
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(200)
        }

        fun isScreenOn(context: Context): Boolean
        {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            return dm.displays.any { it.state != Display.STATE_OFF }
        }
        /*
        <uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
         */
    }
    /**
     * SuperSU wrapper
     */
    object SU
    {
        var processSU: Process? = null
        var errorSU: BufferedReader? = null
        var readerSU: BufferedReader? = null
        var writerSU: OutputStream? = null
        var bEnableSU = false

        fun checkRootAccess():Boolean
                = SU.open() != null

        fun hasRootProcess():Boolean
                = bEnableSU

        fun open(): Process?
        {
            synchronized(bEnableSU)
            {
                try{
                    val exit = processSU?.exitValue()
                    if (exit != null) close()
                }catch (e:Exception){}

                if (processSU != null)
                    return processSU

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
            }
            return processSU
        }

        fun exec(cmd: String): Boolean
        {
            SU.open()

            synchronized(bEnableSU)
            {
                try {
                    writerSU?.write("$cmd\n".toByteArray())
                    writerSU?.flush()
                }catch (e:Exception){
                    e.printStackTrace()
                    return false
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d("GestureDetect exec", "$cmd\n")
            }

            return true
        }

        fun close()
        {
            synchronized(bEnableSU)
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
        }

        fun getFileLine(name: String): String?
        {
            synchronized(bEnableSU) {
                if (exec("echo $(cat $name)")) return readExecLine()
            }
            return null
        }

        fun isFileExists(name: String): Boolean
        {
            synchronized(bEnableSU) {
                if (exec("[ -f $name ] && echo 1 || echo 0")) {
                    return readExecLine() == "1"
                }
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
