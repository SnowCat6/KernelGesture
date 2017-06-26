package gesture.action

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import gesture.GestureService
import gesture.action.ActionItem

/**
 * Action common class
 */
interface ActionApp : ActionItem
{
    var applicationInfo: ApplicationInfo?

    fun init(context: Context, intent: Intent){
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        // This is the default browser's packageName
        applicationInfo = resolveInfo.activityInfo.applicationInfo
    }

    override fun action(): String
            = applicationInfo?.packageName ?: ""

    override fun name(context: Context): String
            = context.packageManager.getApplicationLabel(applicationInfo).toString()

    override fun icon(context: Context): Drawable {
        if (applicationInfo == null) return super.icon(context)
        return context.packageManager.getApplicationIcon(applicationInfo)
    }

    override fun run(context: Context): Boolean
    {
        if (applicationInfo == null) return false

        GestureService.UI.screenON(context)
        GestureService.UI.screenUnlock(context)

        return GestureService.UI.startNewActivity(context, applicationInfo!!.packageName)
    }
}