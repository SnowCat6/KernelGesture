package SuperSU

import android.util.Log
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
       val EVENT_UPDATE = "UPDATE-ROOT"
    }

    fun checkRootAccess():Boolean
            = open() != null

    fun hasRootProcess():Boolean
            = commonSU.bEnableSU

    fun enable(bEnable:Boolean) {
        commonSU.bEnableCheck = bEnable
    }

    fun open(): Process?
    {
        if (!commonSU.bEnableCheck)
            return commonSU.processSU

        try{
            val exit = commonSU.processSU?.exitValue()
            if (exit != null) close()
        }catch (e:Exception){}

        if (commonSU.processSU != null)
            return commonSU.processSU

        synchronized(commonSU.bEnableSU)
        {
            try {
                commonSU.processSU = Runtime.getRuntime().exec("su")
                commonSU.readerSU = commonSU.processSU?.inputStream?.bufferedReader()
                commonSU.errorSU = commonSU.processSU?.errorStream?.bufferedReader()
                commonSU.writerSU = commonSU.processSU?.outputStream

                exec("id")
                val id = readExecLine()?.contains("root")
                if (id == null || id == false) close()

            } catch (e: Exception) {
                e.printStackTrace()
                close()
            }
            commonSU.bEnableSU = commonSU.processSU != null
            commonSU.bEnableCheck = commonSU.bEnableSU
        }
        return commonSU.processSU
    }

    fun exec(cmd: String): Boolean
    {
        open()

        try {
            commonSU.writerSU?.apply {
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
        if (commonSU.processSU == null) return
        try {
            commonSU.processSU?.destroy()
        }catch (e:Exception){
            e.printStackTrace()
        }
        commonSU.processSU = null
        commonSU.writerSU = null
        commonSU.readerSU = null
        commonSU.errorSU = null

        commonSU.bEnableSU = false
    }

    fun getFileLine(name: String): String?
            = if (exec("echo $(cat $name)")) readExecLine() else null

    fun isFileExists(name: String): Boolean
            = exec("[ -f $name ] && echo 1 || echo 0") && readExecLine() == "1"

    fun readExecLine(): String?
    {
        try {
            return  commonSU.readerSU?.readLine()
        }catch (e:Exception){
            e.printStackTrace()
        }
        return null
    }

    fun readErrorLine() : String? {
        try {
            return  commonSU.errorSU?.readLine()
        }catch (e:Exception){
            e.printStackTrace()
        }
        return null
    }
    fun writeErr(value:String):Boolean
        = exec("echo $value>&2")
}

