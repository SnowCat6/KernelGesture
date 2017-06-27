package gesture.action

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import gesture.GestureAction

/**
 * Action common class
 */
abstract class ActionApp(action: GestureAction) : ActionItem(action)
{
    var applicationInfo: ApplicationInfo? = null

    fun onCreate(intent: Intent)
    {
        val resolveInfo = action.context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        // This is the default browser's packageName
        applicationInfo = resolveInfo.activityInfo.applicationInfo
    }

    override fun action(): String
            = applicationInfo?.packageName ?: ""

    override fun name(): String
            = action.context.packageManager.getApplicationLabel(applicationInfo).toString()

    override fun icon(): Drawable {
        if (applicationInfo == null) return super.icon()
        return action.context.packageManager.getApplicationIcon(applicationInfo)
    }

    override fun run(): Boolean
    {
        if (applicationInfo == null) return false

        GestureAction.UI.screenON(action.context)
        GestureAction.UI.screenUnlock(action.context)

        return GestureAction.UI.startNewActivity(action.context, applicationInfo!!.packageName)
    }
}