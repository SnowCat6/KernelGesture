package gesture

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/**
 * Action common class
 */
interface ActionApp : ActionItem
{
    var applicationInfo: ApplicationInfo?

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