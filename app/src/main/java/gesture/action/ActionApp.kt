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
    var intent:Intent? = null

    fun getInfo():ApplicationInfo?
    {
        if (applicationInfo == null) {
            try {
                val resolveInfo = action.context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                applicationInfo = resolveInfo?.activityInfo?.applicationInfo
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        return applicationInfo
    }

    override fun action(): String
            = getInfo()?.packageName ?: ""

    override fun name(): String {
        if (getInfo() == null) return super.name()
        return action.context.packageManager.getApplicationLabel(getInfo()).toString()
    }

    override fun icon(): Drawable {
        if (getInfo() == null) return super.icon()
        return action.context.packageManager.getApplicationIcon(getInfo())
    }

    override fun run(): Boolean
    {
        if (applicationInfo == null) return false

        GestureAction.UI.screenON(action.context)
        GestureAction.UI.screenUnlock(action.context)

        return GestureAction.UI.startNewActivity(action.context, applicationInfo!!.packageName)
    }
}