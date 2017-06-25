package gesture

import android.content.Context
import android.graphics.drawable.Drawable
import ru.vpro.kernelgesture.R

/**
 * Created by Костя on 25.06.2017.
 */

class GestureAction
{
    interface ActionItem
    {
        fun action():String
        fun isAction(context: Context, action: String): Boolean = action == action()
        fun name(context: Context): String
        fun icon(context: Context): Drawable
        fun run(context: Context): Boolean
    }

    companion object
    {
        private val allActions = arrayOf<ActionItem>(ActionScreenOn())

        fun getAction(context:Context, action:String):ActionItem?
                = allActions.firstOrNull { it.isAction(context, action)  }

        fun getActions():Array<ActionItem> = allActions
    }

    class ActionScreenOn :ActionItem
    {
        override fun action():String =  "screen.on"

        override fun name(context: Context): String
                = context.getString(R.string.ui_screen_on)

        override fun icon(context: Context): Drawable
                = context.getDrawable(R.drawable.icon_screen_on)

        override fun run(context: Context): Boolean {
            GestureService.UI.screenON(context)
            return true
        }
    }
}
