package gestureDetect

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.os.Vibrator
import android.view.Display

class GestureHW(val context:Context)
{
    /*
        <uses-permission android:name="android.permission.WAKE_LOCK" />
    */
    fun screenON()
    {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "KernelGesture")
        wakeLock.acquire(500)
    }
    fun powerON()
    {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "KernelGestureCPU")
        wakeLock.acquire(1000)
    }
    /*
    <uses-permission android:name="android.permission.VIBRATE"/>
     */
    fun vibrate(){
        // Vibrate for 500 milliseconds
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(200)
    }

    fun isScreenOn(): Boolean
    {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return dm.displays.any { it.state != Display.STATE_OFF }
    }
    /*
    <uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
     */
}