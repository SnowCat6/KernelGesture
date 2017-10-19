package ru.vpro.kernelgesture.detect.detectors

import SuperSU.ShellSU
import android.arch.lifecycle.LiveData
import android.content.Context
import android.media.RingtoneManager
import gestureDetect.GestureService
import gestureDetect.drivers.SensorInput
import gestureDetect.tools.GestureHW
import gestureDetect.tools.RxInputReader
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

class EventDetector(private val context: Context,
                    private val su : ShellSU)
    : LiveData<List<String>>()
{
    private val composites      = CompositeDisposable()
    private val rxInputReader   = RxInputReader(su).apply { create(context) }

    override fun onInactive() {
        super.onInactive()
        if (!hasObservers()){
            composites.clear()
        }
    }

    private var onCompleteAction : (()->Unit)? = null
    fun onComplete(action : ()->Unit){
        onCompleteAction = action
    }

    //  Start detecting
    fun start() = apply {
        if (composites.size() > 0) return this
        threadAction()
    }
    //  Detect function
    private fun threadAction()
    {
        val hw = GestureHW(context)
        hw.vibrate()

        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        GestureService.bDisableService = true
        val events = mutableListOf<String>()
        rxInputReader.setDevices(SensorInput.getInputEvents().map { it.first })

        composites += rxInputReader
            .observeOn(AndroidSchedulers.mainThread())
            .doOnDispose {
                GestureService.bDisableService = false
                onCompleteAction?.invoke()
            }
            .subscribe {
                if (!events.contains(it.evButton))
                {
                    events.add(it.evButton)
                    value = events
                    hw.vibrate()
                }
            }
    }
}