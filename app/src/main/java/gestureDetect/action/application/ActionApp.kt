package gestureDetect.action.application

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.action.ActionItem

/**
 * Action common class
 */
abstract class ActionApp(action: GestureAction) : ActionItem(action)
{
    private var applicationInfo: ApplicationInfo? = null
    var intent:Intent? = null

    override fun onCreate(context: Context): Boolean = false

    fun onDetect(intent:Intent?): Boolean {
        this.intent = intent
        return true
    }

    open fun getInfo(context: Context):ApplicationInfo?
    {
        if (applicationInfo == null)
        {
            try {
                val resolveInfo = context.packageManager
                        ?.resolveActivity(
                                intent,
                                PackageManager.MATCH_DEFAULT_ONLY)

                applicationInfo = resolveInfo?.activityInfo?.applicationInfo
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        return applicationInfo
    }

    override fun action(context: Context): String?
            = getInfo(context)?.packageName ?: ""

    fun action(context: Context, appAction: String): String
            = if (getInfo(context) != null) appAction else ""

    override fun name(context: Context): String? {
        if (getInfo(context) == null) return super.name(context)
        return context.packageManager?.getApplicationLabel(getInfo(context)).toString()
    }

    override fun icon(context: Context): Drawable? {
        if (getInfo(context) == null) return super.icon(context)
        return context.packageManager?.getApplicationIcon(getInfo(context))
    }

    override fun run(context: Context): Boolean
    {
        if (applicationInfo == null) return false

        with(action){
            screenON(context)
            screenUnlock()
            return startNewActivity(context, applicationInfo!!.packageName)
        }
    }
}