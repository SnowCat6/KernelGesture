package ru.vpro.kernelgesture.detect.detectors

import SuperSU.ShellSU
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.media.RingtoneManager
import gestureDetect.GestureService
import gestureDetect.tools.GestureHW
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers

class EventDetector(val context: Context, val su : ShellSU) :
        LiveData<List<String>>()
{
    val composites              = CompositeDisposable()
    private fun threadAction()  = Observable.create<List<String>>{

        GestureService.bDisableService = true
        val hw = GestureHW(context)
        hw.vibrate()

        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        evCmd(0, 1, 10)
        evCmd(1, 2, 8)
        evCmd(2, 4, 4)
        evCmd(3, 8, 2)

        val events = mutableListOf<String>()
        while(hasObservers()){

            val line = su.readErrorLine() ?: break
            if (line == "--END_DETECT--") break

            if (!line.contains("EV_KEY")) continue

            if (events.contains(line)) continue
            events.add(line)
            it.onNext(events)
            hw.vibrate()
        }

        GestureService.bDisableService = false

        su.close()
        su.open()
        it.onComplete()
    }

    private fun evCmd(ix : Int, nLimit:Int, nRepeat:Int)
    {
        val CR = "\\\\n"
        val seq = (1..nRepeat).joinToString(" ")
        su.exec("while true ; do v$ix=\$(getevent -c $nLimit -l) ; [ \"\$v$ix\" ] && for i in $seq ; do echo \"\$v$ix\">&2 ; done ; done &")
    }

    override fun observe(owner: LifecycleOwner?, observer: Observer<List<String>>?) {
        super.observe(owner, observer)

        if (composites.size() > 0) return

        composites += threadAction()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { composites.clear() }
                .subscribe { value = it.subList(0, it.size) }
    }
}