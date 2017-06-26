package gesture.action

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionWebBrowser(override val context: Context) : ActionApp
{
    override var applicationInfo: ApplicationInfo? = null

    init{
        onCreate(Intent("android.intent.action.VIEW", Uri.parse("http://")))
    }

    override fun action(): String
            = if (applicationInfo?.packageName != null) "application.browser" else ""

    override fun name(): String = context.getString(R.string.ui_web_browser)
}