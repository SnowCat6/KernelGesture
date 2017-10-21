package ru.vpro.kernelgesture.tools.adapter

import android.view.ViewGroup
import android.widget.TextView
import ru.vpro.kernelgesture.tools.TwoString

class HolderItemTwoString(parent: ViewGroup) :
        RecyclerAdapterT.HolderBinder<Any>(parent, android.R.layout.simple_list_item_2)
{
    private val text1   = itemView.findViewById<TextView>(android.R.id.text1)
    private val text2   = itemView.findViewById<TextView>(android.R.id.text2)

    override fun bind(item: Any) {
        super.bind(item)
        item as TwoString
        text2.text = item.content
        text1.text = item.title
    }
}