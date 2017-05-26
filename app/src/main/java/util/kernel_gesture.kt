package util

import java.io.*
import android.R.attr.process
import android.content.Context
import android.os.PowerManager
import android.system.Os.*
import android.util.Log
import java.lang.Compiler.command
import java.util.*
import kotlin.concurrent.thread

class GestureDetect()
{
    val onGesture:Event<String> = Event()
    var devices = emptyMap<String,String>()
    var bStartWait = false
    var processSU:Process? = null

    fun detectGesture():Boolean
    {
        devices = emptyMap()
        if (su() == null) return false

        try {
            var device:Pair<String,String>?
            val lines: List<String> = BufferedReader(FileReader("/proc/bus/input/devices")).readLines()

            device = findDevicePath("mtk-tpd", lines)
            if (device != null) devices += device

            device = findDevicePath("qwerty2", lines)
            if (device != null) devices += device

            device = findDevicePath("ft5x06_ts", lines)
            if (device != null) devices += device

        }catch (e:Exception){}

        return devices.isNotEmpty()
    }
    private fun findDevicePath(name:String, lines: List<String>):Pair<String,String>?
    {
        var findName:String = ""

        for (line in lines)
        {
            if (findName.isNotEmpty())
            {
                val ix = line.indexOf("Handlers=")
                if (ix >= 0){
                    val a = line.substring(ix + 9).split(" ")
                    return Pair(name, a[1])
                }
            }

            val ix = line.indexOf("Name=")
            if (ix < 0) continue

            val n = line.substring(ix+5).trim('"')
            if (n != name) findName = ""
            else findName = n
        }
        return null
    }

    class Event<T> {
        private val handlers = arrayListOf<(Event<T>.(T) -> Unit)>()
        operator fun plusAssign(handler: Event<T>.(T) -> Unit) { handlers.add(handler) }
        fun invoke(value: T) {
            Runnable {
                for (handler in handlers) handler(value)
            }.run()
        }
    }

    fun startWait()
    {
        if (detectGesture()) {
            bStartWait = true;
            thread {
                while (bStartWait) {
                    if (!startWaitThread())
                        stopWait()
                }
            }
        }
    }
    fun stopWait(){
        bStartWait = false
    }
    private fun startWaitThread():Boolean
    {
        for (device in devices)
        {
            if (!bStartWait) return false

            try {
                val cmd = "getevent -c 1 -l /dev/input/${device.value}"
                if (!exec(cmd)) return false

                val line = readExecLine()
                if (!bStartWait) return false
                if (line == null) return false;

                Log.d("GestureDetect", line)
                when(device.key){
                    "mtk-tpd" -> {
                        val arg = line.replace(Regex("\\s+"), " ").split(" ")
                        if (arg[0] == "EV_KEY") onGesture.invoke(arg[1])
                    }
                }

                return true

            }catch (e: Exception) {
                return false
            }
        }
        return false
    }
    private fun su():Process?
    {
        try {
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

        Log.d("GestureDetect exec", "$cmd\n")
        val su:Process = processSU!!
        val out = DataOutputStream(su.outputStream)
        out.writeBytes("$cmd\n")
        out.flush()

        return true
    }
    private fun readExecLine():String?
    {
        return su()?.inputStream?.bufferedReader()?.readLine()
    }
    /*
    ADD Manifest:
        <uses-permission android:name="android.permission.WAKE_LOCK" />
    */
    fun screenON(context:Context)
    {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG")
        wakeLock.acquire()
        wakeLock.release()
    }
}
