package gestureDetect.action.application

import android.content.Context
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
    override fun onCreate(context: Context): Boolean
            = super.onDetect(Intent("android.intent.action.VOICE_ASSIST"))

    override fun action(context: Context): String?
            = super.action(context, "google.ok")

    override fun name(context: Context): String?
            = context.getString(R.string.ui_action_ok_google)

    override fun icon(context: Context): Drawable?
            = context.getDrawableEx(R.drawable.icon_ok_google)

    override fun run(context: Context): Boolean
    {
        with(action){
            screenON(context)
            screenUnlock()
            return startNewActivity(context, intent!!)
        }
    }
}