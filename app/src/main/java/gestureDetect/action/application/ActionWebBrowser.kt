package gestureDetect.action.application

import android.content.Context
import android.content.Intent
import android.net.Uri
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionWebBrowser(action: GestureAction) : ActionApp(action)
{
    override fun onCreate(context: Context): Boolean
            = super.onDetect(Intent(Intent.ACTION_VIEW, Uri.parse("http://")))

    override fun action(context: Context): String?
            = super.action(context, "application.browser")

    override fun name(context: Context): String? = context.getString(R.string.ui_action_web_browser)
}