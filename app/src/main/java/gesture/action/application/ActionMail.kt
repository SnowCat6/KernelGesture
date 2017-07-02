package gesture.action.application

import android.content.Intent
import android.net.Uri
import gesture.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionMail(action: GestureAction) : ActionApp(action)
{
    init{
        intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    }

    override fun action(): String
            = ""//super.action("application.email")

    override fun name(): String
            = context.getString(R.string.ui_action_mail)
}