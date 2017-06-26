package gesture.action

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionCamera(override val context: Context) : ActionApp
{
    override var applicationInfo: ApplicationInfo? = null

    init{
        onCreate(Intent("android.media.action.IMAGE_CAPTURE"))
    }
    override fun action(): String
            = if (applicationInfo?.packageName != null) "application.camera" else ""

    override fun name(): String
            = context.getString(R.string.ui_action_camera)
}