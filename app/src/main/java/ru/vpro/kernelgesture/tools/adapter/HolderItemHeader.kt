package ru.vpro.kernelgesture.tools.adapter

import android.view.ViewGroup
import android.widget.TextView
import ru.vpro.kernelgesture.R

class HolderItemHeader(parent: ViewGroup) :
RecyclerAdapterT.HolderBinder<Any>(parent, R.layout.list_item_header)
{
    private val text1   = itemView.findViewById<TextView>(android.R.id.text1)

    override fun bind(item: Any) {
        super.bind(item)
        text1.text = item.toString()
    }
}