package gestureDetect.action.application

import android.content.Context
import android.content.Intent
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionDialer(action: GestureAction) : ActionApp(action)
{
    override fun onCreate(context: Context): Boolean
            = super.onDetect(Intent(Intent.ACTION_DIAL))

    override fun action(context: Context): String?
            = super.action(context, "application.dialer")

    override fun name(context: Context): String? = context.getString(R.string.ui_action_dialer)
}