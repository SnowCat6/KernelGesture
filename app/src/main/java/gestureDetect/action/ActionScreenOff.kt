package gestureDetect.action

import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.GestureHW
import ru.vpro.kernelgesture.R

/**
 * Screen OFF
 */
class ActionScreenOff(action: GestureAction) : ActionItem(action)
{
    override fun action(): String
            = "screen.off"

    override fun name(): String
            = context.getString(R.string.ui_action_screen_off)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_screen_off)

    override fun run(): Boolean {
        GestureHW(context).screenOFF()
        return true
    }
}