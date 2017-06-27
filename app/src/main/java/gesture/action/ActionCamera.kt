package gesture.action

import android.content.Intent
import gesture.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionCamera(action: GestureAction) : ActionApp(action)
{
    init{
        onCreate(Intent("android.media.action.IMAGE_CAPTURE"))
    }
    override fun action(): String
            = if (applicationInfo?.packageName != null) "application.camera" else ""

    override fun name(): String
            = action.context.getString(R.string.ui_action_camera)
}