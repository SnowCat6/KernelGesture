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
    companion object
    {
        private var processSU: Process? = null
        private var errorSU: BufferedReader? = null
        private var readerSU: BufferedReader? = null
        private var writerSU: OutputStream? = null

        private var bEnableSU = false
        private var bEnableCheck = true

        val EVENT_UPDATE = "UPDATE-ROOT"
    }

    fun checkRootAccess():Boolean
            = open() != null

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
        open()

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

