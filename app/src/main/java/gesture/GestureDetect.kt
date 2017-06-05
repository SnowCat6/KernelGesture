package gesture

import java.io.*
import android.content.*
import android.os.PowerManager
import android.preference.PreferenceManager
import android.util.Log
import android.media.RingtoneManager
import android.net.Uri
import android.os.Vibrator
import ru.vpro.kernelgesture.BuildConfig
import android.view.Display
import android.hardware.display.DisplayManager
import android.os.Handler
import android.widget.Toast
import android.os.Looper





class GestureDetect() {

    private var processSU: Process? = null
    private var errorSU: BufferedReader? = null
    private var readerSU: BufferedReader? = null
    private var writerSU: OutputStream? = null

    private var _bLock = false
    public var lock: Boolean
        get() = _bLock
        set(value) {
            _bLock = value
    }

    private var devices = emptyArray<Pair<String, InputHandler>>()
    private val inputHandlers = arrayOf(InputMTK(), InputKPD(), InputQCOMM_KPD())

    init {
        su()
        detectGesture()
    }


    fun detectGesture(): Boolean
    {
        if (devices.isNotEmpty()) return true
        devices = emptyArray()

        try {
            val lines = BufferedReader(FileReader("/proc/bus/input/devices")).readLines()
            inputHandlers.forEach {
                val input = findDevicePath(it, lines) ?: return@forEach
                devices += Pair(input, it)
            }
        } catch (e: Exception) {
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
                        if (ev.length > 5 && ev.substring(0, 5) == "event") return ev
                    }
                    return null
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
        su()

        while (!lock)
        {
            val line = getEvent(context, devices) ?: continue
            if (lock) return null
            return line

            if (BuildConfig.DEBUG) {
                Log.d("Read line", line)
            }

            for ((first, second) in devices)
            {
                val name = "/dev/input/$first: "
                if (name in line) {
                    val ev = line.substring(name.length)
                    if (BuildConfig.DEBUG) {
                        Log.d("Event detected", line)
                    }
                   return second.onEvent(ev) ?: continue
                }
            }
        }
        return null
    }

    private fun getEvent(context:Context, inputs: Array<Pair<String, InputHandler>>): String?
    {
        while(errorSU?.ready()!!) errorSU?.readLine()

        inputs.forEach { (first, second) ->
            exec("while (getevent -c 4 -l /dev/input/$first 1>&2)  ; do echo /dev/input/$first >&2  ; done &")
        }

        while(!lock)
        {
            var device = ""
            var lines = emptyArray<String>()
            do{
                while(true)
                {
                    val line = errorSU?.readLine() ?: break // readExecLine() ?: break
                    Log.d("EVENT READ", line)
                    if (line.contains("/dev/input/")) {
                        device = line
                        break
                    }
                    if (!line.contains("EV_")) continue
                    if (!line.contains("EV_KEY")) continue
                    lines += line
                }
            }while(!lock && lines.isEmpty())

            for(line in lines)
            {
                for ((first, second) in devices)
                {
 /*
                    val gesture = second.onEvent(line) ?: continue
                    exec("kill %%")
                    return gesture
*/
                    val name = "/dev/input/$first"
                    if (name !in device) continue
                    val ev = line
                    if (BuildConfig.DEBUG) {
                        Log.d("Event detected", line)
                    }
                    val gesture = second.onEvent(line) ?: break
                    exec("kill %%")
                    return gesture
                }
            }
        }
        exec("kill %%")
        return null
    }
    fun toast(context:Context, value:String)
    {
        screenON(context)
        vibrate(context)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, value, Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileLine(name: String): String? {
        if (isFileExists(name) && exec("cat $name")) return readExecLine()
        return null
    }

    private fun isFileExists(name: String): Boolean {
        if (exec("test -e $name") && exec("echo $?")) {
            return readExecLine() == "0"
        }
        return false
    }

    private fun su(): Process? {
        try {
            if (processSU == null) {
                processSU = Runtime.getRuntime().exec("su")
                readerSU = processSU?.inputStream?.bufferedReader()
                errorSU = processSU?.errorStream?.bufferedReader()
                writerSU = processSU?.outputStream
            }
            return processSU
        } catch (e: Exception) {
            e.printStackTrace()
        }
        processSU = null
        return null
    }

    private fun exec(cmd: String): Boolean
    {
        writerSU?.write("$cmd\n".toByteArray())
        writerSU?.flush()

        if (BuildConfig.DEBUG) {
            Log.d("GestureDetect exec", "$cmd\n")
        }

        return true
    }

    private fun readExecLine(): String?
    {
        try {
            return  readerSU?.readLine()
        }catch (e:Exception){
            e.printStackTrace()
        }
        return null
    }

    private fun runGesture(key:String?, convert:Array<Pair<String,String>>? = null):String?
    {
        if (key == null || key.isEmpty())
            return null;

        var keys:Array<Pair<String,String>>? = convert
        if (keys == null)
        {
            //  Allowed standard key
            keys = arrayOf<Pair<String,String>>(
                    Pair("KEY_UP",      "KEY_UP"),
                    Pair("KEY_DOWN",    "KEY_DOWN"),
                    Pair("KEY_LEFT",    "KEY_LEFT"),
                    Pair("KEY_RIGHT",   "KEY_RIGHT"),
                    Pair("KEY_U",       "KEY_U"),
                    Pair("KEY_C",       "KEY_C"),
                    Pair("KEY_O",       "KEY_O"),
                    Pair("KEY_W",       "KEY_W"),
                    Pair("KEY_E",       "KEY_E"),
                    Pair("KEY_V",       "KEY_V"),
                    Pair("KEY_M",       "KEY_M"),
                    Pair("KEY_Z",       "KEY_Z"),
                    Pair("KEY_S",       "KEY_S"),
                    Pair("KEY_VOLUMEUP",    "KEY_VOLUMEUP"),
                    Pair("KEY_VOLUMEDOWN",  "KEY_VOLUMEDOWN")
            )
        }

        for ((first, second) in keys) {
            if (key != first) continue
            return second
        }

        return null
    }

    interface InputHandler
    {
        fun onDetect(name:String):Boolean
        fun onEvent(line:String):String?
        fun setEnable(enable:Boolean){}
        fun getEnable():Boolean{ return false }
    }
    /*
    MT touchscreen with gestures
     */
    inner open class InputMTK: InputHandler
    {
        override fun onDetect(name:String):Boolean{
            return name == "mtk-tpd"
        }
        override fun onEvent(line:String):String?
        {
            val arg = line.replace(Regex("\\s+"), " ").split(" ")
            if (arg[0] == "EV_KEY") return runGesture(arg[1])
            return null
        }
    }

    /*
    MTK and QCOMM keyboard
     */
    inner open class InputKPD : InputHandler
    {
        var HCT_GESTURE_IO:String? = null
        //  HCT version gesture for Android 5x and Android 6x
        val HCT_GESTURE_PATH = arrayOf(
                "/sys/devices/platform/mtk-tpd",
                "/sys/devices/bus/bus\\:touch@")

        override fun setEnable(enable:Boolean)
        {
            if (HCT_GESTURE_IO != null)
                exec("echo on > $HCT_GESTURE_IO/tpgesture_status")
        }

        override fun onDetect(name:String): Boolean
        {
            if (name != "mtk-kpd") return false
            for (it in HCT_GESTURE_PATH) {
                if (!isFileExists(it)) continue
                HCT_GESTURE_IO = it
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
            val gs = getFileLine("$HCT_GESTURE_IO/tpgesture")
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
    inner open class InputQCOMM_KPD : InputHandler
    {
        override fun onDetect(name:String):Boolean
                = name == "qpnp_pon"

        override fun onEvent(line: String): String? {
            val arg = line.replace(Regex("\\s+"), " ").split(" ")
            if (arg[0] != "EV_KEY") return null
            return runGesture(arg[1])
        }
    }

    companion object {

        fun getAllEnable(context: Context): Boolean
        {
            return getEnable(context, "GESTURE_ENABLE")
        }
        fun setAllEnable(context: Context, value: Boolean) {
            setEnable(context, "GESTURE_ENABLE", value)
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

            var action: String? = null
            try {
                action = sharedPreferences.getString("${key}_ACTION", null)
            } catch (e: Exception) { }

            if (action == null || action.isEmpty()) return null
            return action
        }

        fun setAction(context: Context, key: String, value: String) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val e = sharedPreferences.edit()
            e.putString("${key}_ACTION", value)
            e.apply()
        }

        /*
            <uses-permission android:name="android.permission.WAKE_LOCK" />
        */
        fun screenON(context:Context)
        {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG")
            wakeLock.acquire(500)
//            wakeLock.release()
        }
        /*
        <uses-permission android:name="android.permission.VIBRATE"/>
         */
        fun vibrate(context:Context){
            // Vibrate for 500 milliseconds
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator?.vibrate(200)
        }
        fun playNotify(context:Context, notify: Uri)
        {
            RingtoneManager.getRingtone(context, notify).play();
        }

        fun isScreenOn(context: Context): Boolean
        {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            return dm.displays.any { it.state != Display.STATE_OFF }
        }
        fun screenUnlock(context: Context){
            // todo: add screen unlock
        }
    }
}
