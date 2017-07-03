package gestureDetect.action.application

import android.content.Intent
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionDialer(action: GestureAction) : ActionApp(action)
{
    override fun onDetect(): Boolean
            = super.onDetect(Intent(Intent.ACTION_DIAL))

    override fun action(): String
            = super.action("application.dialer")

    override fun name(): String = context.getString(R.string.ui_action_dialer)
}