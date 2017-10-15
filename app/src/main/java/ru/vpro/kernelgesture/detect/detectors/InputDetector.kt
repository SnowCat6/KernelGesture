package ru.vpro.kernelgesture.detect.detectors

import SuperSU.ShellSU
import android.arch.lifecycle.LiveData
import android.content.Context
import gestureDetect.drivers.SensorInput
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import ru.vpro.kernelgesture.BuildConfig


class InputDetector (private val context: Context,
                     private val su : ShellSU)
    : LiveData<List<String>>() {

    private val composites = CompositeDisposable()
    private val thisValue  = mutableListOf<String>()

    private fun threadAction()  = Observable.create<String> { emitter ->

        emitter.apply {

            su.close()
            su.open()

            onNext("Android SDK:" + android.os.Build.VERSION.SDK_INT)
            onNext("Device name:" + android.os.Build.MODEL)

            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            onNext("App version:${pInfo.versionName}")

            onNext(String())

            onNext("Add input devices list")
            SensorInput.getInputEvents().forEach {
                onNext("device:${it.second}=>${it.first}")
            }

            if (!su.checkRootAccess())
            {
                onNext("No ROOT access to more search, please install SuperSU")
                return@apply
            }

            if (BuildConfig.DEBUG) {
                return@apply
            }
            onNext(String())

            onNext("Run find cmd to search /sys/ devices")
            doSearch(this, su, "/sys", listOf("*gesture*", "*gesenable*", "*wakeup_mode*"))

            onNext("Run find cmd to search /proc/ functions")
            doSearch(this, su, "/proc", listOf("*goodix*"))

            /**
            //  Xiaomi gesture mode???
            // ln -s /sys/devices/soc.0/78b8000.i2c/i2c-4/4-0038/wakeup_mode /data/tp/wakeup_mode
            //  ln -s /sys/devices/soc.0/78b8000.i2c/i2c-4/4-004a/wakeup_mode /data/tp/wakeup_mode
             */
            onNext("Run find cmd to search /data/tp/ devices")
            doSearch(this, su, "/data/tp", listOf("*wakeup*"))
        }
        emitter.onComplete()
    }

    private fun doSearch(emitter : ObservableEmitter<String>,
                         su:ShellSU, path:String,
                         search: List<String>) : Boolean
    {
        if (!hasObservers()) return true

        var files = emptyList<String>()

        if (!su.execExists("find")){
            emitter.onNext("No \"find\" command found, try setup \"BusyBox\" and repeat!")
/*
            var rawCmd = emptyList<String>()
            search.forEach { rawCmd += "-e $it" }
            val cmd = rawCmd.joinToString(" ")

            if (!su.exec("ls -lR $path | grep $cmd")) return false
            if (!su.exec("echo --END--")) return false

            while (true) {
                val line = su.readExecLine() ?: break
                if (line == "--END--") break

                val split = line.split(Regex("\\s\\d{2}:\\d{2}\\s"), 2)
                if (split.size == 2) files += split[1]
            }
            files.forEach {
                log += "path:$it"
            }
 */
        }else {
            var rawCmd = emptyList<String>()
            search.forEach { rawCmd += "-name $it" }
            val cmd = rawCmd.joinToString(" -or ")

            if (!su.exec("find $path $cmd")) return false
            if (!su.exec("echo --END--")) return false

            while (hasObservers()) {
                val line = su.readExecLine() ?: break
                if (line == "--END--") break

                files += line
            }
            files.forEach {
                val value = su.getFileLine(it)
                emitter.onNext("path:$it=>$value")
            }
        }

        return true
    }

    private var onCompleteAction : (()->Unit)? = null
    fun onComplete(action : ()->Unit){
        onCompleteAction = action
    }

    fun start()  = apply{
        if (composites.size() > 0) return this

        thisValue.clear()
        value = thisValue
        composites += threadAction()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    composites.clear()
                    onCompleteAction?.invoke()
                }
                .subscribe {
                    thisValue.add(it)
                    value = thisValue
                }
    }
}