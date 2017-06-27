package gesture.action

import android.content.Intent
import android.net.Uri
import gesture.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionWebBrowser(action: GestureAction) : ActionApp(action)
{
    init{
        onCreate(Intent("android.intent.action.VIEW", Uri.parse("http://")))
    }

    override fun action(): String
            = if (applicationInfo?.packageName != null) "application.browser" else ""

    override fun name(): String = action.context.getString(R.string.ui_web_browser)
}