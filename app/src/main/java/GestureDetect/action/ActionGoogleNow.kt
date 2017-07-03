package GestureDetect.action

import android.content.Intent
import android.graphics.drawable.Drawable
import GestureDetect.GestureAction
import ru.vpro.kernelgesture.R

/**
 * Action OK Google
 */
class ActionGoogleNow(action: GestureAction) : ActionItem(action)
{
    override fun action(): String
            = "google.ok"

    override fun name(): String
            = context.getString(R.string.ui_ok_google)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_ok_google)

    override fun run(): Boolean
    {
        action.screenON()
        action.screenUnlock()
        val googleNowIntent = Intent("android.intent.action.VOICE_ASSIST")
        return action.startNewActivity(googleNowIntent)
    }
}