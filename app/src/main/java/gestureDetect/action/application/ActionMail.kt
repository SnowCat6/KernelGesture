package gestureDetect.action.application

import android.content.Context
import android.content.Intent
import android.net.Uri
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionMail(action: GestureAction) : ActionApp(action)
{
    override fun onCreate(context: Context): Boolean
            = super.onDetect(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")))

    override fun action(context: Context): String?
            = super.action(context, "application.email")

    override fun name(context: Context): String?
            = context.getString(R.string.ui_action_mail)
}