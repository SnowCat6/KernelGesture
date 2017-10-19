package ru.vpro.kernelgesture.tools

import android.content.Context
import android.preference.Preference
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.adapter_choose_item.view.*
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.SettingsActivity

class BoxAdapter (val preference: Preference) : BaseAdapter()
{
    private var objects = emptyList<Any>()
    private var lInflater : LayoutInflater? = null
    private var currentItem : Any? = null
    private var ctx : Context? = null

    fun setItems(items: List<Any>) {
        objects = items
        notifyDataSetChanged()
    }
    fun setCurrent(item : Any?){
        currentItem = item
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
        val context = parent.context
        ctx = context

        val inflater = lInflater?:
                LayoutInflater.from(parent.context).also { lInflater = it }

        val thisItem = getItem(position)
        val bSplitView: View? = convertView?.findViewById(R.id.splitter)

        if (thisItem == "-")
        {
            if (bSplitView != null) return convertView
            return inflater.inflate(R.layout.adapter_splitter, parent, false)
        }
        // используем созданные, но не используемые view
        val view: View = if (bSplitView != null || convertView == null)
            inflater.inflate(R.layout.adapter_choose_item, parent, false)
        else convertView

        view.title?.text = SettingsActivity.uiName(context, thisItem)
        view.icon?.setImageDrawable(SettingsActivity.uiIcon(context, thisItem))

        view.background = if (currentItem == thisItem)
            context.getDrawableEx(android.R.color.holo_orange_light)
        else null

        view.alpha = if (isEnabled(position)) 1f else 0.35f

        return view
    }

    override fun isEnabled(position: Int): Boolean {
        return ctx?.let {
            SettingsActivity.uiEnable(it, getItem(position))
        } ?: false
    }

}