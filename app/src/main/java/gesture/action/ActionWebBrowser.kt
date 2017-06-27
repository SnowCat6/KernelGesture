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
        intent = Intent("android.intent.action.VIEW", Uri.parse("http://"))
    }

    override fun action(): String
            = if (getInfo() != null) "application.browser" else ""

    override fun name(): String = action.context.getString(R.string.ui_web_browser)
}