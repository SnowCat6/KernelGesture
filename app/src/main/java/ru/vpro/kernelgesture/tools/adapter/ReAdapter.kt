package ru.vpro.kernelgesture.tools.adapter

import org.inowave.planning.ui.common.adapter.HolderItemAny

class ReAdapter(items : List<Any>? = null) : RecyclerAdapterT<Any>(items)
{
    init {
        bindClass(Any::class.java, HolderItemAny::class.java)
    }
}