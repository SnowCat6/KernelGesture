package gestureDetect.tools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import gestureDetect.GestureService

/*
 <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 */
class OnBootGestureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent)
    {
        if(!listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED).contains(intent.action)) return

        val settings = GestureSettings(context)
        if (!settings.getAllEnable()) return
        context.startService(Intent(context, GestureService::class.java))
    }
}