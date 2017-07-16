package SuperSU

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import ru.vpro.kernelgesture.BuildConfig
import java.io.BufferedReader
import java.io.OutputStream

/**
 * SuperSU wrapper
 */
class ShellSU
{
    class ProcessSU
    {
        var processSU: Process? = null
        var errorSU: BufferedReader? = null
        var readerSU: BufferedReader? = null
        var writerSU: OutputStream? = null
        var bEnableSU = false
        var bEnableCheck = true
    }
    companion object Common
    {
       val commonSU = ProcessSU()
       val EVENT_UPDATE_ROOT_STATE = "UPDATE_ROOT_STATE"
    }

    val su = commonSU

    fun checkRootAccess(context: Context? = null):Boolean
    {
        val bEnable = su.processSU != null
        if (open() == null){
            if (bEnable) context?.apply {
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(Intent(EVENT_UPDATE_ROOT_STATE))
            }
            return false
        }
        if (bEnable) return true

        context?.apply {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(EVENT_UPDATE_ROOT_STATE))
        }

        return true
    }

    fun hasRootProcess():Boolean
            = su.bEnableSU

    fun enable(bEnable:Boolean) {
        su.bEnableCheck = bEnable
    }

    fun open(): Process?
    {
        with(su)
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

                    if (exec("id")) {
                        val id = readExecLine()?.contains("root")
                        if (id == null || id == false) close()
                    }else{
                        close()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    close()
                }
                bEnableSU = processSU != null
                bEnableCheck = bEnableSU

                return processSU
            }
        }
    }

    fun exec(cmd: String): Boolean
    {
        try {
            su.writerSU?.apply {
                write("$cmd\n".toByteArray())
                flush()
            }
        }catch (e:Exception){
            e.printStackTrace()
            FirebaseCrash.report(e)
            return false
        }

        if (BuildConfig.DEBUG) {
            Log.d("GestureDetect exec", "$cmd\n")
        }

        return true
    }

    fun close()
    {
        with(su)
        {
            synchronized(bEnableSU)
            {
                if (processSU == null) return
                try {
                    processSU?.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                processSU = null
                writerSU = null
                readerSU = null
                errorSU = null

                bEnableSU = false
            }
        }
    }

    fun getFileLine(name: String): String?
            = if (exec("echo $(cat $name)")) readExecLine() else null

    fun isFileExists(name: String): Boolean
            = exec("[ -f $name ] && echo 1 || echo 0") && readExecLine() == "1"

    fun readExecLine(): String?
    {
        try {
            return  su.readerSU?.readLine()
        }catch (e:Exception){
            e.printStackTrace()
        }
        return null
    }

    fun readErrorLine() : String? {
        try {
            return  su.errorSU?.readLine()
        }catch (e:Exception){
            e.printStackTrace()
            FirebaseCrash.report(e)
        }
        return null
    }
    fun writeErr(value:String):Boolean
        = exec("echo $value>&2")
}

