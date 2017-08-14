package gestureDetect.action.application

import android.content.Intent
import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawableEx

/**
 * Action OK Google
 */
class ActionGoogleNow(action: GestureAction) : ActionApp(action)
{
    override fun onDetect(): Boolean
            = super.onDetect(Intent("android.intent.action.VOICE_ASSIST"))

    override fun action(): String
            = super.action("google.ok")

    override fun name(): String
            = context.getString(R.string.ui_action_ok_google)

    override fun icon(): Drawable
            = context.getDrawableEx(R.drawable.icon_ok_google)

    override fun run(): Boolean
    {
        with(action){
            screenON()
            screenUnlock()
            return startNewActivity(intent!!)
        }
    }
}