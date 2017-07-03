package GestureDetect.action.application

import android.content.Intent
import android.net.Uri
import GestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionWebBrowser(action: GestureAction) : ActionApp(action)
{
    init{
        intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
    }

    override fun action(): String
            = super.action("application.browser")

    override fun name(): String = context.getString(R.string.ui_web_browser)
}