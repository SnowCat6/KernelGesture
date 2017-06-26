package gesture.action

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import gesture.GestureService
import ru.vpro.kernelgesture.R

/**
 * Action OK Google
 */
class ActionGoogleNow(override val context: Context) : ActionItem {
    override fun action(): String = "google.ok"

    override fun name(): String
            = context.getString(R.string.ui_ok_google)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_ok_google)

    override fun run(): Boolean
    {
        GestureService.UI.screenON(context)
        GestureService.UI.screenUnlock(context)
        val googleNowIntent = Intent("android.intent.action.VOICE_ASSIST")
        return GestureService.UI.startNewActivity(context, googleNowIntent)
    }
}