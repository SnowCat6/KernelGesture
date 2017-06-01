package gesture

import android.app.admin.DevicePolicyManager
import java.io.*
import android.content.*
import android.os.PowerManager
import android.preference.PreferenceManager
import android.util.Log
import kotlin.concurrent.thread
import android.content.Context.VIBRATOR_SERVICE
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Debug
import android.os.Vibrator
import ru.vpro.kernelgesture.BuildConfig
import android.content.Context.POWER_SERVICE
import android.view.Display
import android.content.Context.DISPLAY_SERVICE
import android.hardware.display.DisplayManager
import android.os.Build




class GestureDetect()
{
    val onGesture:Event<String> = Event()

    private var bStartWait = false
    private var processSU:Process? = null

    private var devices = emptyArray<Pair<String,InputHandler>>()
    private val inputHandlers = arrayOf(InputMTK(), InputKPD(), InputQCOMM_TPD())

    init {
        detectGesture()
    }

    fun detectGesture():Boolean
    {
        devices = emptyArray()
        if (su() == null) return false

        try {
            val lines = BufferedReader(FileReader("/proc/bus/input/devices")).readLines()
            inputHandlers.forEach {
                val input = findDevicePath(it, lines) ?: return@forEach
                devices += Pair(input, it)
            }
        }catch (e:Exception){}

        return devices.isNotEmpty()
    }
    fun clear(){
        onGesture.clear()
        stopWait()
        processSU?.destroy()
        processSU = null
    }
    private fun findDevicePath(handler:InputHandler, lines: List<String>):String?
    {
        var bThisEntry = false

        for (line in lines)
        {
            if (bThisEntry)
            {
                val ix = line.indexOf("Handlers=")
                if (ix >= 0){
                    val a = line.substring(ix + 9).split(" ")
                    return a.firstOrNull { it.substring(0, 5) == "event" }
                }
            }

            val ix = line.indexOf("Name=")
            if (ix < 0) continue

            val n = line.substring(ix+5).trim('"')
            bThisEntry = handler.onDetect(n)
        }
        return null
    }

    class Event<T> {
        private val handlers = arrayListOf<(Event<T>.(T) -> Unit)>()
        operator fun plusAssign(handler: Event<T>.(T) -> Unit) { handlers.add(handler) }
        fun invoke(value: T) = Runnable {
            for (handler in handlers) handler(value)
        }.run()
        fun clear () = handlers.clear()
    }

    fun startWait()
    {
        if (bStartWait) return;
        bStartWait = detectGesture()
        if (su() == null){
            stopWait()
            return
        }
        thread {
            for (it in devices) it.second.setEnable(true)
            while (bStartWait && startWaitThread()) {}
            stopWait()
        }
    }
    fun stopWait(){
        bStartWait = false
        processSU?.outputStream?.write(3) // Control-C
        processSU?.outputStream?.flush()
    }
    private fun startWaitThread():Boolean
    {
        while(bStartWait){
            val line = getEvent(null) ?: return false
            if (!bStartWait) return false

            devices.forEach { (first, second) ->
                val name = "/dev/input/$first: "
                if (name in line) {
                    val ev = line.substring(name.length)
                    if (BuildConfig.DEBUG) {
                        Log.d("Gesture detect", line)
                    }
                    return second.onEvent(this, ev)
                }
            }
        }
        return false
    }
    private fun getEvent(input:String?):String?
    {
        try {
            if (input != null) {
                if (!exec("getevent -c 1 -l /dev/input/$input")) return null
                return readExecLine()
            }
            if (!exec("getevent -c 1 -l | grep EV_")) return null
            return readExecLine()

        }catch (e:Exception){
            Log.d("Gesture read", e.toString())
        }
        return null;
    }
    private fun getFileLine(name:String):String?
    {
        if (!isFileExists(name)) return null
        if (!exec("cat $name")) return null
        return readExecLine()
    }
    private fun isFileExists(name:String):Boolean
    {
        if (!exec("test -e $name")) return false
        return suExitCode() == "0"
    }
    private fun suExitCode():String
    {
        if (!exec("echo $?")) return ""
        return readExecLine()
    }
    private fun su():Process?
    {
        try {
//            if (processSU == null) processSU = Runtime.getRuntime().exec("sh")
            if (processSU == null) processSU = Runtime.getRuntime().exec("su")
            try {
                if (processSU != null && processSU!!.exitValue() != 0) processSU = null;
            }catch (e:Exception){
                return processSU
            }
            return processSU
        }catch (e:Exception){}

        processSU = null
        return null
    }
    private fun exec(cmd:String): Boolean
    {
        if (su() == null) return false

        if (BuildConfig.DEBUG) {
            Log.d("GestureDetect exec", "$cmd\n")
        }
        val su:Process = processSU!!
        val out = DataOutputStream(su.outputStream)
        out.writeBytes("$cmd\n")
        out.flush()

        return true
    }
    private fun readExecLine():String
            = su()!!.inputStream.bufferedReader().readLine()

    private fun runGesture(key:String?, convert:Array<Pair<String,String>>? = null):Boolean
    {
        if (key == null || key.isEmpty())
            return false;

        if (convert == null){
            onGesture.invoke(key)
            return true
        }

        for ((first, second) in convert) {
            if (key != first) continue
            onGesture.invoke(second)
            return true
        }

        return false
    }

    interface InputHandler
    {
        fun onDetect(name:String):Boolean
        fun onEvent(detector:GestureDetect, line:String):Boolean
        fun setEnable(enable:Boolean){}
        fun getEnable():Boolean{ return false }
    }
    /*
    MT touchscreen with gestures
     */
    open class InputMTK: InputHandler
    {
        override fun onDetect(name:String):Boolean{
            return name == "mtk-tpd"
        }
        override fun onEvent(detector:GestureDetect, line:String):Boolean{
            val arg = line.replace(Regex("\\s+"), " ").split(" ")
            if (arg[0] == "EV_KEY" && arg[2] == "DOWN") detector.runGesture(arg[1])
            return true
        }
    }

    /*
    MTK and QCOMM keyboard
     */
    inner open class InputKPD : InputHandler
    {
        //  HCT version gesture for Android 5x and Android 6x
        val HCT_GESTURE_PATH = arrayOf(
                "/sys/devices/platform/mtk-tpd",
                "/sys/devices/bus/bus\\:touch@")

        override fun setEnable(enable:Boolean){
           HCT_GESTURE_PATH.forEach { exec("echo on > $it/tpgesture_status") }
        }

        override fun onDetect(name:String):Boolean
                = name == "mtk-kpd" || name == "qpnp_pon"

        override fun onEvent(detector:GestureDetect, line:String):Boolean
        {
            val arg = line.replace(Regex("\\s+"), " ").split(" ")
            if (arg[0] != "EV_KEY"|| arg[2] != "DOWN") return true

            when(arg[1]){
                "KEY_PROG3" ->  if (onEventHCT()) return true
                "KEY_VOLUMEUP",
                "KEY_VOLUMEDOWN" ->  return runGesture(arg[1])
            }
            if (onEventOKK(detector, arg[1])) return true

            return true
        }
        //  HCT gesture give from file
        private fun onEventHCT():Boolean
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

            //  get gesture name
            for (it in HCT_GESTURE_PATH) {
                val gs = getFileLine("$it/tpgesture")
                if (gs != null) return runGesture(gs, keys)
            }
            return false
/*
            var gs = detector.getFileLine("/sys/devices/platform/mtk-tpd/tpgesture")
            if (gs == null) gs = detector.getFileLine("/sys/devices/bus/bus\\:touch@/tpgesture")

            return detector.runGesture(gs, keys)
 */      }
        //  Oukitel K4000 device gesture
        private fun onEventOKK(detector:GestureDetect, key:String):Boolean
        {
            val keys = arrayOf<Pair<String,String>>(
                    Pair("BTN_JOYSTICK","KEY_U"),
                    Pair("BTN_THUMB",   "KEY_C"),
                    Pair("BTN_THUMB2",  "KEY_E"),
                    Pair("BTN_TOP2",    "KEY_O"),
                    Pair("012c",        "KEY_S"),
                    Pair("BTN_BASE6",   "KEY_V"),
                    Pair("BTN_BASE4",   "KEY_W"),
                    Pair("BTN_BASE5",   "KEY_Z")
            )
            return detector.runGesture(key, keys)
        }
    }

    /*
    Qualcomm touchscreen ft5x06_ts for testing
     */
    inner open class InputQCOMM_TPD : InputKPD()
    {
        override fun onDetect(name:String):Boolean
                = name == "ft5x06_ts"
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
            wakeLock.acquire(10)
            wakeLock.release()
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
