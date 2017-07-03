package GestureDetect.action.application

import android.content.Intent
import android.graphics.drawable.Drawable
import GestureDetect.GestureAction
import GestureDetect.action.application.ActionApp
import android.content.pm.PackageManager
import ru.vpro.kernelgesture.R

/**
 * Action OK Google
 */
class ActionGoogleNow(action: GestureAction) : ActionApp(action)
{
    override fun onDetect(): Boolean {
        intent = Intent("android.intent.action.VOICE_ASSIST")
        return super.onDetect()
    }

    override fun action(): String
            = super.action("google.ok")

    override fun name(): String
            = context.getString(R.string.ui_ok_google)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_ok_google)

    override fun run(): Boolean
    {
        with(action){
            screenON()
            screenUnlock()
            return startNewActivity(intent!!)
        }
    }
}