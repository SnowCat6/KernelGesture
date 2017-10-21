package ru.vpro.kernelgesture.tools.adapter

import android.support.design.widget.Snackbar
import org.inowave.planning.common.tools.toClipboard
import org.inowave.planning.ui.common.adapter.*
import ru.vpro.kernelgesture.R
import ru.vpro.kernelgesture.tools.HeaderString
import ru.vpro.kernelgesture.tools.TwoString

class ReAdapter(items : List<Any>? = null)
    : RecyclerAdapterT<Any>(items)
{
    init {
        bindClass(Any::class.java,          HolderItemAny::class.java)
        bindClass(HeaderString::class.java, HolderItemHeader::class.java)
        bindClass(TwoString::class.java,    HolderItemTwoString::class.java)

        onClickItem { view, index, item ->

            val context = view.context
            context.toClipboard(item.toString())
            Snackbar.make(view, context.getString(R.string.ui_snack_clipboard), Snackbar.LENGTH_SHORT)
                    .show()
        }
    }
}