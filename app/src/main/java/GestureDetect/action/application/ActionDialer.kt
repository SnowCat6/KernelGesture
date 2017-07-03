package GestureDetect.action.application

import android.content.Intent
import GestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionDialer(action: GestureAction) : ActionApp(action)
{
    init{
        intent = Intent(Intent.ACTION_DIAL)
//        intent?.data = Uri.parse("tel:123456789")
    }

    override fun action(): String
            = super.action("application.dialer")

    override fun name(): String = context.getString(R.string.ui_action_dialer)
}