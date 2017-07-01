package gesture.action

import android.content.Intent
import android.graphics.drawable.Drawable
import gesture.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action OK Google
 */
class ActionGoogleNow(action: GestureAction) : ActionItem(action)
{
    override fun action(): String
            = "google.ok"

    override fun name(): String
            = action.context.getString(R.string.ui_ok_google)

    override fun icon(): Drawable
            = action.context.getDrawable(R.drawable.icon_ok_google)

    override fun run(): Boolean
    {
        action.screenON()
        action.screenUnlock()
        val googleNowIntent = Intent("android.intent.action.VOICE_ASSIST")
        return action.startNewActivity(googleNowIntent)
    }
}