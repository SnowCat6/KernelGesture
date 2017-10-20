package ru.vpro.kernelgesture.tools.adapter

import org.inowave.planning.ui.common.adapter.*

class ReAdapter(items : List<Any>? = null) : RecyclerAdapterT<Any>(items)
{
    init {
        bindClass(Any::class.java,          HolderItemAny::class.java)
        bindClass(HeaderString::class.java, HolderItemHeader::class.java)
        bindClass(TwoString::class.java,    HolderItemTwoString::class.java)
    }
}