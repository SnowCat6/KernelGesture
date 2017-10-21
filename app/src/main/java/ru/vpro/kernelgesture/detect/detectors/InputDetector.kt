package ru.vpro.kernelgesture.detect.detectors

import SuperSU.ShellSU
import android.arch.lifecycle.LiveData
import android.content.Context
import android.os.Build
import gestureDetect.drivers.SensorInput
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import ru.vpro.kernelgesture.tools.HeaderString
import ru.vpro.kernelgesture.tools.TwoString
import ru.vpro.kernelgesture.BuildConfig


class InputDetector (private val context: Context,
                     private val su : ShellSU)
    : LiveData<List<Any>>() {

    private val composites = CompositeDisposable()
    private val thisValue  = mutableListOf<Any>()

    private fun threadAction()  = Observable.create<Any> { emitter ->

        emitter.apply {

            onNext(HeaderString("Device info"))

            onNext(TwoString("Android SDK", Build.VERSION.SDK_INT.toString()))
            onNext(TwoString("Device name", Build.MODEL))

            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            onNext(TwoString("App version", pInfo.versionName))

            onNext(HeaderString("Input devices list"))
            SensorInput.getInputEvents().forEach {
                onNext(TwoString(it.first, it.second))
            }

            if (!su.checkRootAccess())
            {
                onNext("No ROOT access to more search, please install SuperSU")
                return@apply
            }

            if (BuildConfig.DEBUG) {
                return@apply
            }

            onNext(HeaderString("Run find cmd to search /sys/ devices"))
            doSearch(this, su, "/sys", listOf("*gesture*", "*gesenable*", "*wakeup_mode*"))

            onNext(HeaderString("Run find cmd to search /proc/ functions"))
            doSearch(this, su, "/proc", listOf("*goodix*"))

            /**
            //  Xiaomi gesture mode???
            // ln -s /sys/devices/soc.0/78b8000.i2c/i2c-4/4-0038/wakeup_mode /data/tp/wakeup_mode
            //  ln -s /sys/devices/soc.0/78b8000.i2c/i2c-4/4-004a/wakeup_mode /data/tp/wakeup_mode
             */
            onNext(HeaderString("Run find cmd to search /data/tp/ devices"))
            doSearch(this, su, "/data/tp", listOf("*wakeup*"))
        }
        emitter.onComplete()
    }

    private fun doSearch(emitter : ObservableEmitter<Any>,
                         su:ShellSU, path:String,
                         search: List<Any>) : Boolean
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
                val value = su.getFileLine(it) ?: return@forEach
                emitter.onNext(TwoString(it, value))
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