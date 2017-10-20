package gestureDetect.tools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.PowerManager
import android.view.Display
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

class RxScreenOn(val context: Context): Observable<Boolean>()
{
    private val observers = mutableListOf<Observer<in Boolean>>()
    private val onEventIntent = object : BroadcastReceiver()
    {
        //  Events for screen on and screen off
        override fun onReceive(context: Context, intent: Intent)
        {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onNext(false)
                Intent.ACTION_SCREEN_ON ->  onNext(true)
            }
        }
    }

    override fun subscribeActual(observer: Observer<in Boolean>?)
    {
        observer?.also {
            val size = synchronized(observers) {
                observers.add(observer)
                val dispose = EvDispose(observer)
                observer.onSubscribe(dispose)
                observers.size
            }
            if (size == 1) onSubscribe()
        }
    }
    inner class EvDispose(private val observer: Observer<in Boolean>)
        : Disposable
    {
        override fun isDisposed(): Boolean {
            return synchronized(observers){
                observers.contains(observer)
            }
        }

        override fun dispose()
        {
            val bHasObservers = synchronized(observers) {
                observers.remove(observer)
                observers.size > 0
            }
            if (!bHasObservers) onDispose()
        }
    }

    private fun onSubscribe()
    {
        synchronized(observers) {
            //  Register screen activity event
            val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
            context.registerReceiver(onEventIntent, intentFilter)
        }
    }
    private fun onDispose()
    {
        synchronized(observers) {
            try {
                context.unregisterReceiver(onEventIntent)
            } catch (e: Exception) {
            }
        }
    }

    private fun onNext(event : Boolean)
    {
        synchronized(observers){ observers.subList(0, observers.size) }
        .forEach { it.onNext(event) }
    }

    fun hasObservers() = observers.size > 0

    fun isScreenOn(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            return displayManager?.displays?.any { it.state != Display.STATE_OFF } == true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isScreenOn
    }
}