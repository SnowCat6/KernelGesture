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
import android.widget.ArrayAdapter
import java.util.*
import java.util.concurrent.Semaphore

class GestureDetect private constructor()
{
    private var _bLock = false
    var lock: Boolean
        get() = _bLock
        set(value) {
            _bLock = value
            if (_bLock){
                closeEvents()
            }
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

    private var supported = emptyArray<String>()
    private var devices = emptyArray<Pair<String, InputHandler>>()
    private val inputHandlers = arrayOf(InputMTK(), InputKPD(), InputQCOMM_KPD(), InputFT5x06_ts())

    init {
        detectDevices()
    }
    fun close(){
        SU.close()
    }


    fun enable(boolean: Boolean){
        if (SU.processSU == null) return
        for(it in devices) it.second.setEnable(boolean)
    }

    private fun detectDevices(): Boolean
    {
        devices = emptyArray()

        try {
            val lines = BufferedReader(FileReader("/proc/bus/input/devices")).readLines()
            inputHandlers.forEach {
                findDevicePath(it, lines) ?: return@forEach
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return devices.isNotEmpty()
    }

    private fun findDevicePath(handler: InputHandler, lines: List<String>): String? {
        var bThisEntry = false

        for (line in lines) {
            if (bThisEntry) {
                val ix = line.indexOf("Handlers=")
                if (ix >= 0) {
                    val a = line.substring(ix + 9).split(" ")
                    for (ev in a) {
                        if (ev.length < 5 || !ev.contains("event")) continue
                        devices += Pair("/dev/input/$ev", handler)
                        bThisEntry = false
                        break
                    }
                    continue
                }
            }

            val ix = line.indexOf("Name=")
            if (ix < 0) continue

            val n = line.substring(ix + 5).trim('"')
            bThisEntry = handler.onDetect(n)
        }
        return null
    }

    fun waitGesture(context:Context): String?
    {
        if (SU.open() == null) return null
        return getEvent(context)
    }

    private var bGetEvents = false
    private fun getEvent(context:Context): String?
    {
        bGetEvents = true
        var device = ""

        //  Flush old output
        try {
            while (SU.errorSU?.ready() == true) SU.readErrorLine()
        }catch (e:Exception){
            e.printStackTrace()
        }

        //  For each device
        devices.forEach { (inputName, device) ->
            //  Power on gesture if available, many drivers not set this value if screen off
            device.setEnable(true)
            //  Run input event detector
            SU.exec("while v=$(getevent -c 4 -l $inputName)  ; do echo $inputName\\\\n\"\$v\">&2 ; done &")
        }

        while(!lock)
        {
            //  Read line from input
            val line = SU.readErrorLine() ?: break

            //  Stop if gesture need stop run
            if (lock) break

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
            for ((first, second) in devices)
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
        //  Error reading, no continue
        return null
    }
    private fun closeEvents()
    {
        if (!bGetEvents) return
        bGetEvents = false

        SU.exec("kill -s SIGINT %%")
    }
    private fun addSupport(value:String)
    {
        if (supported.contains(value)) return
        supported += value
    }
    private fun addSupport(value:Array<String>)
            = value.forEach { addSupport(it) }

    fun getSupport():Array<String>
    {
        detectDevices()
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

    private fun toast(context:Context, value:String)
    {
        screenON(context)
        vibrate(context)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, value, Toast.LENGTH_LONG).show()
        }
    }

    private fun runGesture(key:String?, convert:Array<Pair<String,String>>? = null):String?
    {
        if (key == null || key.isEmpty())
            return null;

        var gesture = key
        if (convert != null) {
            for ((first, second) in convert) {
                if (key != first) continue
                gesture = second
                break
            }
        }

        //  Allowed standard key
        val allowGestures = arrayOf(
                "KEY_UP",
                "KEY_DOWN",
                "KEY_LEFT",
                "KEY_RIGHT",
                "KEY_U",
                "KEY_C",
                "KEY_O",
                "KEY_W",
                "KEY_E",
                "KEY_V",
                "KEY_M",
                "KEY_Z",
                "KEY_S",
                "KEY_VOLUMEUP",
                "KEY_VOLUMEDOWN"
        )
        return if (gesture in allowGestures) gesture else null
    }

    private interface InputHandler
    {
        fun onDetect(name:String):Boolean
        fun onEvent(line:String):String?
        fun setEnable(enable:Boolean){}
        fun getEnable():Boolean{ return false }
    }
    /*
    MT touchscreen with gestures
     */
    private inner open class InputMTK: InputHandler
    {
        override fun onDetect(name:String):Boolean{
            return name.toLowerCase() == "mtk-tpd"
        }
        override fun onEvent(line:String):String?
        {
            val arg = line.replace(Regex("\\s+"), " ").split(" ")
            if (arg[0] == "EV_KEY") return runGesture(arg[1])
            return null
        }
    }

    private inner class GS(
            public val detectFile: String,
            public val setPowerON: String,
            public val setPowerOFF: String,
            public val getGesture: String
    );

    /*
    MTK and QCOMM keyboard
     */
    private inner open class InputKPD : InputHandler
    {
        var HCT_GESTURE_IO:GS? = null
        //  HCT version gesture for Android 5x and Android 6x
        val HCT_GESTURE_PATH = arrayOf(
                //  Android 5.x HCT gestures
                GS("/sys/devices/platform/mtk-tpd/tpgesture_status",
                        "on > /sys/devices/platform/mtk-tpd/tpgesture_status",
                        "off > /sys/devices/platform/mtk-tpd/tpgesture_status",
                        "/sys/devices/platform/mtk-tpd/tpgesture"),
                // Android 6.x HCT gestures
                GS("/sys/devices/bus/bus\\:touch@/tpgesture_status",
                        "on > /sys/devices/bus/bus\\:touch@/tpgesture_status",
                        "off > /sys/devices/bus/bus\\:touch@/tpgesture_status",
                        "/sys/devices/bus/bus\\:touch@/tpgesture"),
                //  Unknown 3.10 FTS touchscreen gestures for driver FT6206_X2605
                GS("/sys/class/syna/gesenable",
                        "1 > /sys/class/syna/gesenable",
                        "0 > /sys/class/syna/gesenable",
                        "")
        )

        override fun setEnable(enable:Boolean)
        {
            if (HCT_GESTURE_IO == null) return

            if (enable) SU.exec("echo ${HCT_GESTURE_IO!!.setPowerON}")
            else SU.exec("echo ${HCT_GESTURE_IO!!.setPowerOFF}")
        }

        override fun onDetect(name:String): Boolean
        {
            if (name.toLowerCase() != "mtk-kpd") return false
            addSupport(arrayOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))

            if (HCT_GESTURE_IO == null) {
                for (it in HCT_GESTURE_PATH) {
                    if (!SU.isFileExists(it.detectFile)) continue
                    HCT_GESTURE_IO = it
                    addSupport(arrayOf("GESTURE"))
                    break
                }
            }
            return true
        }

        override fun onEvent(line:String):String?
        {
            val arg = line.replace(Regex("\\s+"), " ").split(" ")
            if (arg[0] != "EV_KEY") return null

            when(arg[1]){
                "KEY_PROG3" ->  return onEventHCT()
                "KEY_VOLUMEUP",
                "KEY_VOLUMEDOWN" ->  return runGesture(arg[1])
            }
            return onEventOKK(arg[1])
        }
        //  HCT gesture give from file
        private fun onEventHCT():String?
        {
            val keys = arrayOf<Pair<String,String>>(
                    Pair("UP",          "KEY_UP"),
                    Pair("DOWN",        "KEY_DOWN"),
                    Pair("LEFT",        "KEY_LEFT"),
                    Pair("RIGHT",       "KEY_RIGHT"),
                    Pair("DOUBCLICK",   "KEY_U"),
                    Pair("c",           "KEY_C"),
                    Pair("o",           "KEY_O"),
                    Pair("w",           "KEY_W"),
                    Pair("e",           "KEY_E"),
                    Pair("v",           "KEY_V"),
                    Pair("m",           "KEY_M"),
                    Pair("z",           "KEY_Z"),
                    Pair("s",           "KEY_S")
            )

            if (HCT_GESTURE_IO == null) return null
            //  get gesture name
            val gs = SU.getFileLine(HCT_GESTURE_IO!!.getGesture)
            return runGesture(gs, keys)
        }
        //  Oukitel K4000 device gesture
        private fun onEventOKK(key:String):String?
        {
            val keys = arrayOf<Pair<String,String>>(
                    Pair("BTN_PINKIE",  "KEY_UP"),
                    Pair("BTN_BASE",    "KEY_DOWN"),
                    Pair("BTN_BASE2",   "KEY_LEFT"),
                    Pair("BTN_BASE3",   "KEY_RIGHT"),
                    Pair("BTN_JOYSTICK","KEY_U"),
                    Pair("BTN_THUMB",   "KEY_C"),
                    Pair("BTN_THUMB2",  "KEY_E"),
                    Pair("BTN_TOP2",    "KEY_O"),
                    Pair("012c",        "KEY_S"),
                    Pair("BTN_BASE6",   "KEY_V"),
                    Pair("BTN_BASE4",   "KEY_W"),
                    Pair("BTN_TOP",     "KEY_M"),
                    Pair("BTN_BASE5",   "KEY_Z")
            )
            return runGesture(key, keys)
        }
    }

    /*
    Qualcomm keys
     */
    private inner open class InputQCOMM_KPD : InputHandler
    {
        override fun onDetect(name:String):Boolean {
            if (name.toLowerCase() != "qpnp_pon" && name.toLowerCase() != "gpio-keys")
                return false

            addSupport(arrayOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))
            return true
        }

        override fun onEvent(line: String): String? {
            val arg = line.replace(Regex("\\s+"), " ").split(" ")
            if (arg[0] != "EV_KEY") return null
            return runGesture(arg[1])
        }
    }

    //  Unknown FT5x06_ts gesture solution
    private inner open class InputFT5x06_ts : InputHandler
    {
        override fun onDetect(name: String): Boolean {
            if (name.toLowerCase() != "ft5x06_ts") return false
            return true
        }

        override fun onEvent(line: String): String? {
            val arg = line.replace(Regex("\\s+"), " ").split(" ")
            if (arg[0] != "EV_KEY") return null
            return runGesture(arg[1])
        }

        override fun setEnable(enable: Boolean) {
            if (enable) {
                SU.exec("echo 1 > sys/class/gesture/gesture_ft5x06/enable")
            }else{
                SU.exec("echo 0 > sys/class/gesture/gesture_ft5x06/enable")
            }
        }

    }

    companion object
    {
        private val gs = GestureDetect()

        fun getInstance():GestureDetect = gs

        fun getAllEnable(context: Context): Boolean
        {
            return getEnable(context, "GESTURE_ENABLE")
        }
        fun setAllEnable(context: Context, value: Boolean) {
            setEnable(context, "GESTURE_ENABLE", value)
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
        fun screenUnlock(context: Context){
            // todo: add screen unlock
        }
        fun checkRootAccess():Boolean
        {
            return SU.open() != null
        }
        fun hasRoot():Boolean
        {
            return SU.processSU != null
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
            val lockSU = Semaphore(1)

            fun open(): Process?
            {
                try{
                    val exit = processSU?.exitValue()
                    if (exit != null) close()
                }catch (e:Exception){}

                if (processSU != null)
                    return processSU

                lockSU.acquire()
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
                lockSU.release()
                return processSU
            }

            fun exec(cmd: String): Boolean
            {
                SU.open()
                try {
                    writerSU?.write("$cmd\n".toByteArray())
                    writerSU?.flush()
                }catch (e:Exception){
                    e.printStackTrace()
                    return false
                }

                if (BuildConfig.DEBUG) {
                    Log.d("GestureDetect exec", "$cmd\n")
                }

                return true
            }
            fun close(){
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
            }

            fun getFileLine(name: String): String? {
                if (exec("echo $(cat $name)")) return readExecLine()
                return null
            }

            fun isFileExists(name: String): Boolean {
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
}
