package util

import java.io.*
import android.R.attr.process
import android.content.*
import android.os.PowerManager
import android.system.Os.*
import android.util.Log
import java.lang.Compiler.command
import java.util.*
import kotlin.concurrent.thread

class GestureDetect(context: Context)
{
    val onGesture:Event<String> = Event()

    private var devices = emptyMap<String,String>()
    private var bStartWait = false
    private var processSU:Process? = null

    init {
        initIndent(context)
        startWait()
    }

    fun detectGesture():Boolean
    {
        devices = emptyMap()
        if (su() == null) return false

        try {
            val lines: List<String> = BufferedReader(FileReader("/proc/bus/input/devices")).readLines()
            for (name in arrayOf("mtk-tpd","qwerty2","ft5x06_ts"))
            {
                val device = findDevicePath(name, lines)
                if (device != null) devices += device
            }

        }catch (e:Exception){}

        return devices.isNotEmpty()
    }
    private fun findDevicePath(name:String, lines: List<String>):Pair<String,String>?
    {
        var findName = ""

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

    private fun startWait()
    {
        bStartWait = detectGesture()
        thread {
            while (bStartWait && startWaitThread())
            stopWait()
        }
    }
    private fun stopWait(){
        bStartWait = false
    }
    private fun startWaitThread():Boolean
    {
        for ((key, value) in devices)
        {
            if (!bStartWait) return false

            try {
                val cmd = "getevent -c 1 -l /dev/input/${value}"
                if (!exec(cmd)) return false

                val line = readExecLine()
                if (!bStartWait) return false
                if (line == null) return false;

                Log.d("Gesture detect", line)
                when(key){
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

    private fun initIndent(context:Context)
    {
        val intentFilter =  IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)

        context.registerReceiver(object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent: Intent)
            {
                when(intent.action)
                {
                    Intent.ACTION_SCREEN_OFF->{
                        Log.d(ContentValues.TAG, Intent.ACTION_SCREEN_OFF)
                        startWait()
                    }
                    Intent.ACTION_SCREEN_ON ->{
                        Log.d(ContentValues.TAG, Intent.ACTION_SCREEN_ON)
                        stopWait()
                    }
                }
            }
        }, intentFilter)

    }
}
