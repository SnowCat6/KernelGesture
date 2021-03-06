package SuperSU

import android.util.Log
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import ru.vpro.kernelgesture.BuildConfig
import java.io.BufferedReader
import java.io.OutputStream

/**
 * SuperSU wrapper
 */
class ShellSU(val su : ProcessSU = commonSU)
{
    companion object Common
    {
        val commonSU = ProcessSU()
    }

    class ProcessSU
    {
        var processSU: Process? = null
        var errorSU: BufferedReader? = null
        var readerSU: BufferedReader? = null
        var writerSU: OutputStream? = null
        var bEnableSU = false
        var bEnableCheck = true

        val rxRootEnable = BehaviorSubject.createDefault(false)
        var rxError  : Observable<String>? = null
    }

    fun checkRootAccess() = open() != null
    fun hasRootProcess() = su.bEnableSU

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

                    writerSU?.apply {
                        write("id\n".toByteArray())
                        flush()
                    }

                    val id = readExecLine()?.contains("root")
                    if (id == null || id == false) close()

                } catch (e: Exception) {
                    e.printStackTrace()
                    close()
                }
                bEnableSU = processSU != null
                bEnableCheck = bEnableSU
            }
            if (rxRootEnable.value != bEnableSU)
                rxRootEnable.onNext(bEnableSU)

            return processSU
        }
    }

    fun exec(cmd: String): Boolean
    {
        if (open() == null) return false

        try {
            su.writerSU?.apply {
                write("$cmd\n".toByteArray())
                flush()
            }
        }catch (e:Exception){
            e.printStackTrace()
//            FirebaseCrash.report(e)
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
                    writerSU?.apply {
                        write("exit\n".toByteArray())
                        flush()
                        Thread.sleep(500)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                processSU = null
                writerSU = null
                readerSU = null
                errorSU = null
                readerSU?.close()
                errorSU?.close()
                processSU?.destroy()

                rxError = null

                bEnableSU = false
                if (rxRootEnable.value != bEnableSU)
                    rxRootEnable.onNext(bEnableSU)
            }
        }
    }

    fun killJobs(){
        exec("kill %%")
    }

    fun getFileLine(name: String): String?
            = if (exec("echo $(cat $name)")) readExecLine() else null

    fun isFileExists(name: String): Boolean
            = exec("[ -f $name ] && echo 1 || echo 0") && readExecLine() == "1"

    fun execExists(cmd:String):Boolean
            = exec("[ $(command -v $cmd) ] && echo 1 || echo 0") && readExecLine() == "1"

    fun readExecLine(): String?
    {
        try {
            return  su.readerSU?.readLine()
        }catch (e:Exception){
            e.printStackTrace()
        }
        return null
    }
    fun readErrorLine() : Observable<String>?
        = su.rxError ?: Observable.create<String> {
        while(!it.isDisposed) {
            su.errorSU?.readLine()
                    ?.apply {it.onNext(this)}
                    ?: break
        }
        it.onComplete()
    }.also { su.rxError = it }

    fun writeErr(value:String):Boolean
        = exec("echo $value>&2")
}

