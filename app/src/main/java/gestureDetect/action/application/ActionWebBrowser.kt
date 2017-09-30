package gestureDetect.action.application

import android.content.Intent
import android.net.Uri
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionWebBrowser(action: GestureAction) : ActionApp(action)
{
    override fun onCreate(): Boolean
            = super.onDetect(Intent(Intent.ACTION_VIEW, Uri.parse("http://")))

    override fun action(): String
            = super.action("application.browser")

    override fun name(): String = context.getString(R.string.ui_action_web_browser)
}