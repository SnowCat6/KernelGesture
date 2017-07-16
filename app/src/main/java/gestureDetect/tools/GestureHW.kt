package gestureDetect.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.os.Vibrator
import android.view.Display


class GestureHW(val context:Context)
{
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    /*
        <uses-permission android:name="android.permission.WAKE_LOCK" />
    */
    fun screenON()
    {
        val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "KernelGesture")
        wakeLock?.acquire(500)
    }
    fun powerON()
    {
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "KernelGestureCPU")
        wakeLock?.acquire(1000)
    }
    /*
    <uses-permission android:name="android.permission.VIBRATE"/>
     */
    fun vibrate(){
        vibrator?.vibrate(200)
    }

    fun isScreenOn(): Boolean
            = displayManager?.displays?.any { it.state != Display.STATE_OFF } ?: false
    /*
    <uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
     */

    var homeScreenActivity = listOf<String>()
    fun isHomeScreen():Boolean
    {
        if (homeScreenActivity.isEmpty())
        {
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_HOME)
            val mPackageManager = context.packageManager

            mPackageManager.queryIntentActivities(mainIntent, 0)
                .forEach {
                    homeScreenActivity += it.activityInfo.packageName
                }
        }

        val mActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = mActivityManager.runningAppProcesses[0].processName

        return homeScreenActivity.firstOrNull {
            it == packageName } != null
    }
}