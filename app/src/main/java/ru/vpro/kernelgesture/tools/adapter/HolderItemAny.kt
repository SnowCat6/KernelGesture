package org.inowave.planning.ui.common.adapter

import android.view.ViewGroup
import android.widget.TextView
import ru.vpro.kernelgesture.tools.adapter.RecyclerAdapterT

/**
 * Класс для отображения любого объекта в виде текстовой строки
 */
class HolderItemAny(parent: ViewGroup) :
        RecyclerAdapterT.viewBinder<Any>(parent, android.R.layout.simple_list_item_1)
{
    private val text1   = itemView.findViewById<TextView>(android.R.id.text1)

    override fun bind(item: Any) {
        super.bind(item)
        text1.text = item.toString()
    }
}