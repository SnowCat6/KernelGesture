package gestureDetect.action.music

import android.content.Intent
import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import gestureDetect.action.ActionItem
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.getDrawableEx

abstract class ActionMusic(action: GestureAction, val cmd:String):ActionItem(action)
{
    companion object
    {
        val SERVICECMD = "com.android.music.musicservicecommand"
        val CMDNAME = "command"
        val CMDTOGGLEPAUSE = "togglepause"
        val CMDSTOP = "stop"
        val CMDPAUSE = "pause"
        val CMDPLAY = "play"
        val CMDPREVIOUS = "previous"
        val CMDNEXT = "next"
    }

    override fun run(): Boolean
    {
        action.vibrate()
        val intent = Intent(SERVICECMD)
        intent.putExtra(CMDNAME, cmd)
        context.sendBroadcast(intent)
        return false
    }
}

class ActionMusicPlayPause(action:GestureAction):
        ActionMusic(action, CMDTOGGLEPAUSE)
{
    override fun action(): String
            = "music.playPause"
    override fun name(): String
            = context.getString(R.string.ui_action_music_playpause)
    override fun icon(): Drawable
            = context.getDrawableEx(R.drawable.icon_music_playstop)
}

class ActionMusicPrev(action:GestureAction):
        ActionMusic(action, CMDPREVIOUS)
{
    override fun action(): String
            = "music.playPrev"
    override fun name(): String
            = context.getString(R.string.ui_action_music_prev)
    override fun icon(): Drawable
            = context.getDrawableEx(R.drawable.icon_music_prev)
}

class ActionMusicNext(action:GestureAction):
        ActionMusic(action, CMDNEXT)
{
    override fun action(): String
            = "music.playNext"
    override fun name(): String
            = context.getString(R.string.ui_action_music_next)
    override fun icon(): Drawable
            = context.getDrawableEx(R.drawable.icon_music_next)
}
