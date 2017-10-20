package gestureDetect.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.os.Vibrator
import android.view.Display
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.os.Build
import android.content.Context.POWER_SERVICE
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import io.reactivex.subjects.BehaviorSubject

class GestureHW(val context:Context)
{
    private val vibrator = context.applicationContext
            .getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val powerManager = context.applicationContext
            .getSystemService(Context.POWER_SERVICE) as? PowerManager

    companion object {
        private var keyguardLock: KeyguardManager.KeyguardLock? = null
    }

    private var bRegistred = false
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
        wakeLock?.acquire(500)
    }
/**
    <uses-permission android:name="android.permission.CONTROL_KEYGUARD" />
    <uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
*/
    fun screenUnlock(){
        if (keyguardLock != null) return
        val keyguardManager = context.applicationContext
                .getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardLock = keyguardManager.newKeyguardLock("KernelGesture")
        keyguardLock?.disableKeyguard()
    }
    fun screenLock(){
        keyguardLock?.reenableKeyguard()
        keyguardLock = null
    }

    /*
    <uses-permission android:name="android.permission.VIBRATE"/>
     */
    fun vibrate(){
        vibrator?.vibrate(200)
    }

    fun isScreenOn(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val displayManager = context.applicationContext
                    .getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            return displayManager?.displays?.any { it.state != Display.STATE_OFF } == true
        }

        val powerManager = context.applicationContext
                .getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isScreenOn
    }
    /*
    <uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
     */
    private var homeScreenActivity = listOf<String>()
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

        val packageName:String
        val mActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//        packageName = mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName()
        packageName = mActivityManager.runningAppProcesses[0].processName

        return homeScreenActivity.firstOrNull { it == packageName } != null
    }
    fun hasProximity():Boolean
    {
        val mSensorManager: SensorManager? = context.applicationContext
                .getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mProximity: Sensor? = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        return mProximity != null
    }
    fun hasVibrate():Boolean
    {
        return vibrator?.hasVibrator() == true
    }
}