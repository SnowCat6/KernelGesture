package gesture.action

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import gesture.GestureAction

/**
 * Action common class
 */
abstract class ActionApp(context:Context) : ActionItem(context)
{
    var applicationInfo: ApplicationInfo? = null

    fun onCreate(intent: Intent)
    {
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        // This is the default browser's packageName
        applicationInfo = resolveInfo.activityInfo.applicationInfo
    }

    override fun action(): String
            = applicationInfo?.packageName ?: ""

    override fun name(): String
            = context.packageManager.getApplicationLabel(applicationInfo).toString()

    override fun icon(): Drawable {
        if (applicationInfo == null) return super.icon()
        return context.packageManager.getApplicationIcon(applicationInfo)
    }

    override fun run(): Boolean
    {
        if (applicationInfo == null) return false

        GestureAction.UI.screenON(context)
        GestureAction.UI.screenUnlock(context)

        return GestureAction.UI.startNewActivity(context, applicationInfo!!.packageName)
    }
}