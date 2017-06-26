package gesture

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import ru.vpro.kernelgesture.R

/**
 * Action default browser
 */
class ActionCamera : ActionApp
{
    override var applicationInfo: ApplicationInfo? = null

    override fun action(): String
            = if (applicationInfo?.packageName != null) "application.camera" else ""

    override fun onStart(context: Context)
    {
        init(context,Intent("android.media.action.IMAGE_CAPTURE"))
    }

    override fun name(context: Context): String
            = context.getString(R.string.ui_action_camera)
}