package gestureDetect.action.application

import android.content.Intent
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionCamera(action: GestureAction) : ActionApp(action)
{
    override fun onCreate(): Boolean
            = super.onDetect(Intent("android.media.action.IMAGE_CAPTURE"))

    override fun action(): String
            = super.action("application.camera")

    override fun name(): String
            = context.getString(R.string.ui_action_camera)
}