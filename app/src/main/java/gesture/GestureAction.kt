package gesture

import android.content.Context


class GestureAction private constructor(val context:Context)
{
    companion object
    {
        var ga:GestureAction? = null
        fun getInstance(context:Context):GestureAction
        {
            if (ga == null) {
                ga = GestureAction(context)
            }
            return ga!!
        }
    }
    private val allActions = arrayOf(
            ActionScreenOn(),
            ActionGoogleNow(),
            ActionSpeechTime(),
            ActionSpeechBattery(),
            ActionWebBrowser(),
            ActionCamera()
    )

    fun onStart() {
       allActions.forEach { it.init(context) }
    }
    fun onStop(){

    }

    fun getAction(action: String): ActionItem?
            = allActions.firstOrNull { it.isAction(context, action) }

    fun getActions(): Array<ActionItem>
            = allActions

}
