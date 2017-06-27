package gesture.action

import android.content.Context
import android.content.Intent
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionCamera(context: Context) : ActionApp(context)
{
    init{
        onCreate(Intent("android.media.action.IMAGE_CAPTURE"))
    }
    override fun action(): String
            = if (applicationInfo?.packageName != null) "application.camera" else ""

    override fun name(): String
            = context.getString(R.string.ui_action_camera)
}