package gestureDetect

import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.os.Vibrator
import android.util.Log
import android.view.Display
import android.app.Activity




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

            val appList = mPackageManager.queryIntentActivities(mainIntent, 0)
            for (i in appList.indices) {
                val apl = appList.get(i)
                homeScreenActivity += apl.activityInfo.packageName
            }
        }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val taskInfo = am.getRunningTasks(1)
        val packageName = taskInfo[0].topActivity.packageName

        return homeScreenActivity.firstOrNull { it == packageName } != null
    }
}