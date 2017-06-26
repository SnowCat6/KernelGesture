package gesture

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionWebBrowser : ActionApp
{
    override var applicationInfo: ApplicationInfo? = null

    override fun action(): String
            = if (applicationInfo?.packageName != null) "application.browser" else ""

    override fun init(context: Context)
    {
        val browserIntent = Intent("android.intent.action.VIEW", Uri.parse("http://"))
        val resolveInfo = context.packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)

        // This is the default browser's packageName
        applicationInfo = resolveInfo.activityInfo.applicationInfo
    }

    override fun name(context: Context): String = context.getString(R.string.ui_web_browser)
}