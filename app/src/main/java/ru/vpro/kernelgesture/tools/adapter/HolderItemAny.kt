package org.inowave.planning.ui.common.adapter

import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.list_item_header.view.*
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.adapter.RecyclerAdapterT

/**
 * Класс для отображения любого объекта в виде текстовой строки
 */
class HolderItemAny(parent: ViewGroup) :
        RecyclerAdapterT.HolderBinder<Any>(parent, android.R.layout.simple_list_item_1)
{
    private val text1   = itemView.findViewById<TextView>(android.R.id.text1)

    override fun bind(item: Any) {
        super.bind(item)
        text1.text = item.toString()
    }
}

data class HeaderString(val title : String){
    override fun toString() =  title
}

class HolderItemHeader(parent: ViewGroup) :
RecyclerAdapterT.HolderBinder<Any>(parent, R.layout.list_item_header)
{
    private val text1   = itemView.findViewById<TextView>(android.R.id.text1)

    override fun bind(item: Any) {
        super.bind(item)
        text1.text = item.toString()
    }
}

data class TwoString(val title : String, val content : String)
{
    override fun toString() =  "$title=>$content"
}

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