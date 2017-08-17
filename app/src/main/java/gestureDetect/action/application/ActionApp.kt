package gestureDetect.action.application

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

    override fun onDetect(): Boolean = false

    fun onDetect(intent:Intent?): Boolean {
        this.intent = intent
        return true
    }

    open fun getInfo():ApplicationInfo?
    {
        if (applicationInfo == null)
        {
            try {
                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                applicationInfo = resolveInfo?.activityInfo?.applicationInfo
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        return applicationInfo
    }

    override fun action(): String
            = getInfo()?.packageName ?: ""

    fun action(appAction:String): String
            = if (getInfo() != null) appAction else ""

    override fun name(): String {
        if (getInfo() == null) return super.name()
        return context.packageManager.getApplicationLabel(getInfo()).toString()
    }

    override fun icon(): Drawable {
        if (getInfo() == null) return super.icon()
        return context.packageManager.getApplicationIcon(getInfo())
    }

    override fun run(): Boolean
    {
        if (applicationInfo == null) return false

        with(action){
            screenON()
            screenUnlock()
            return startNewActivity(applicationInfo!!.packageName)
        }
    }
}