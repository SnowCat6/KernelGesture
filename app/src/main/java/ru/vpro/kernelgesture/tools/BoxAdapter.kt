package ru.vpro.kernelgesture.tools

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.preference.Preference
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import gestureDetect.GestureAction
import gestureDetect.tools.GestureSettings
import kotlinx.android.synthetic.main.adapter_choose_item.view.*
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.SettingsActivity
import kotlin.concurrent.thread

class BoxAdapter constructor(
        val preference: Preference) : BaseAdapter()
{
    private var objects = emptyList<Any>()
    private val context = preference.context
    private val lInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val settings = GestureSettings(context)
    private val currentAction = settings.getAction(preference.key)
            ?.let { if (it.isEmpty()) null else it }

    class ActionListItem(val action:String?, val name:String, val icon: Drawable)
    {
        constructor(context: Context, action : Any)
                : this(SettingsActivity.uiAction(context, action), SettingsActivity.uiName(context, action), SettingsActivity.uiIcon(context, action))
    }

    init{
        objects += ActionListItem(context, "none")

        val actions = GestureAction.getInstance(context)
        actions.getActions(context)
                .forEach { objects += ActionListItem(context, it) }

        thread {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val pkgAppsList = pm.queryIntentActivities(mainIntent, 0)

            var items = listOf<Any>("-")
            var filterMap = listOf<String>()
            pkgAppsList.forEach {
                if (!filterMap.contains(it.activityInfo.applicationInfo.packageName)) {
                    items += ActionListItem(context, it.activityInfo.applicationInfo)
                    filterMap += it.activityInfo.applicationInfo.packageName
                }
            }

            Handler(Looper.getMainLooper()).post {
                objects += items
                notifyDataSetChanged()
            }
        }
    }

    //  Количество объектов
    override fun getCount(): Int = objects.size
    //  Объект
    override fun getItem(position: Int): Any = objects[position]
    // id по позиции
    override fun getItemId(position: Int): Long = position.toLong()
    // пункт списка
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
    {
        val thisItem = getItem(position)
        val bSplitView: View? = convertView?.findViewById(R.id.splitter)

        if (thisItem == "-")
        {
            if (bSplitView != null) return convertView
            return lInflater.inflate(R.layout.adapter_splitter, parent, false)
        }
        // используем созданные, но не используемые view
        val view: View = if (bSplitView != null || convertView == null)
            lInflater.inflate(R.layout.adapter_choose_item, parent, false)
        else convertView

        view.title?.text = SettingsActivity.uiName(context, thisItem)
        view.icon?.setImageDrawable(SettingsActivity.uiIcon(context, thisItem))

        val action = SettingsActivity.uiAction(context, thisItem)
        view.background = if (currentAction == action)
            context.getDrawableEx(android.R.color.holo_orange_light)
        else null

        if (action == "") view.alpha = 0.35f
        else view.alpha = 1f

        return view
    }

    override fun isEnabled(position: Int): Boolean {
        return SettingsActivity.uiAction(context, getItem(position)) != ""
    }
}