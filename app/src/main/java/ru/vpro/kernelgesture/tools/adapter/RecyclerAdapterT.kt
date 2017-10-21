package ru.vpro.kernelgesture.tools.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.collections.ArrayList

open class RecyclerAdapterT<T>(items: List<T>? = null) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>()
        where T : Any
{

    private var refItems            = items

    private val thisItems           = ArrayList<T>()
    private val thisFiltered        = ArrayList<T>()

    private val thisHeaders         = ArrayList<View>()
    private val thisFooters         = ArrayList<View>()

    private var nSelectMode         = SELECT_MODE_NONE
            val selected            = ArrayList<T>()
    private val thisHoldersSelected = ArrayList<HolderBinder<T>>()

    private val binderMap           = ArrayList<Pair<Class<*>, Class<*>>>()
    private var onBindTypeAction    : ((T) -> Class<*>?)? = null

    private var itemClickHandler    : ((View, Int, T) -> Unit)? = null
    private var itemFilterHandler   : ((T) -> Boolean)? = null

    interface IViewHolder<T> where T : Any
    {
        fun bind(item: T)
        fun onClick(adapter : RecyclerAdapterT<T>, item : T)
        fun onSelect(item: T, bSelected: Boolean)
        fun hasDivider():Boolean
    }

    open class HolderBinder<T>(parent: ViewGroup, nLayout: Int) :
            RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context)
                    .inflate(nLayout, parent, false)
            ) where T : Any
    {
        open fun bind(item: T) {}
        open fun onClick(adapter : RecyclerAdapterT<T>, item : T) {
            adapter.onItemSelect(this, item)
        }

        open fun onSelect(item: T, bSelected: Boolean) {
            if (bSelected)
                itemView.setBackgroundColor(0x33FFFFFF)
            else
                itemView.setBackgroundColor(0)
        }

        open fun hasDivider():Boolean = true
    }

    class HeaderHolder(internal var layout: FrameLayout) :
            RecyclerView.ViewHolder(layout)
    {
        init {
            layout.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        fun bind(v: View) {
            v.parent?.apply { (this as? ViewGroup?)?.removeView(v) }
            layout.removeAllViews()
            layout.addView(v)
        }
    }

    class AdapterDivider(context: Context):
            RecyclerView.ItemDecoration()
    {
        private var mDivider: Drawable

        init{
            val styledAttributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
            mDivider = styledAttributes.getDrawable(0)
            styledAttributes.recycle()
        }
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?)
        {
            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight

            val adapter     = parent.adapter as? RecyclerAdapterT<*> ?: return
            for (i in 0 until parent.childCount - 1)
            {
                // Чертить только между элементами списка
                val child = parent.getChildAt(i)

                val nAdapterIX = parent.getChildAdapterPosition(child)

                //	Headers
                if (adapter.getItemViewType(nAdapterIX) < 0) continue
                // Footers
                if (adapter.getItemViewType(nAdapterIX + 1) < 0) break

                //  Не рисовать если не разрешено
                val thisHolder = parent.findViewHolderForAdapterPosition(nAdapterIX) as? HolderBinder<*>? ?: continue
                val nextHolder = parent.findViewHolderForAdapterPosition(nAdapterIX + 1) as HolderBinder<*>? ?: continue

                if (thisHolder.hasDivider() && !nextHolder.hasDivider()) continue
                if (!thisHolder.hasDivider() && !nextHolder.hasDivider()) continue

                val params = child.layoutParams as? RecyclerView.LayoutParams? ?: continue

                super.onDraw(c, parent, state)

                val top = child.bottom + params.bottomMargin
                val bottom = top + mDivider.intrinsicHeight

                mDivider.setBounds(left, top, right, bottom)
                mDivider.draw(c)
            }
        }
    }
    init {
        items?.apply {
            thisItems.addAll(this)
            thisFiltered.addAll(this)
        }
    }
    fun bindClass(clazz : Class<*>, clazzHolder : Class<*>){
        val ix = binderMap.indexOfFirst { it.first == clazz && it.second == clazzHolder }
        if (ix < 0) binderMap += Pair(clazz, clazzHolder)
    }
    fun unbindAll(){
        binderMap.clear()
    }
    fun onBindViewClass(action: (T)->Class<*>?){
        onBindTypeAction = action
    }

    fun filter(filter: (T) -> Boolean)
    {
        itemFilterHandler = filter
        selected.clear()
        refresh()
    }

    fun onClickItem(listener : (view:View, index:Int, item : T) -> Unit){
        itemClickHandler = listener
    }

    var items: List<T>
        get() = thisFiltered
        set(value)
        {
            refItems = value
            thisItems.clear()
            thisItems.addAll(value)
            selected.clear()

            refreshAll()
        }

    fun setSelectMode(nMode: Int) {
        if (nMode == nSelectMode) return
        nSelectMode = nMode
        notifyDataSetChanged()
    }

    fun setSelected(item: T)
    {
        if (selected.contains(item)) return
        val ix = thisFiltered.indexOf(item)
        if (ix < 0) return

        if (nSelectMode == SELECT_MODE_NONE) return
        if (nSelectMode == SELECT_MODE_SINGLE)
        {
            val oldSel = ArrayList(selected)
            selected.clear()
            oldSel.forEach {
                notifyItemChanged(thisFiltered.indexOf(it) + thisHeaders.size)
            }
        }
        selected.add(item)
        notifyItemChanged(ix + thisHeaders.size)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        return when (viewType) {
            TYPE_HEADER, TYPE_FOOTER -> HeaderHolder(FrameLayout(parent.context))
            else -> onCreateView(parent, viewType)!!
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
    {
        when (getItemViewType(position)) {
            TYPE_HEADER -> {
                (holder as? HeaderHolder)?.bind(thisHeaders[position])
                return
            }
            TYPE_FOOTER -> {
                (holder as? HeaderHolder)?.bind(thisFooters[position - thisHeaders.size - thisFiltered.size])
                return
            }
        }

        val item = getThisItem(position)?:return
        val itemHolder = holder as? HolderBinder<T>?

        itemHolder?.itemView?.setOnClickListener {
            val nPos = thisFiltered.indexOfFirst { it == item }
            if (nPos >= 0) {
                itemHolder.onClick(this, item)
                itemClickHandler?.invoke(itemHolder.itemView, nPos+thisHeaders.size, item)
            }
        }

        val bSelectedItem = selected.contains(item)
        val bSelectedHolder = thisHoldersSelected.contains(itemHolder)

        if (bSelectedHolder != bSelectedItem) {
            itemHolder?.apply {
                onSelect(item, bSelectedItem)
                if (bSelectedItem && !bSelectedHolder)
                    thisHoldersSelected.add(this)
                else
                    thisHoldersSelected.remove(this)
            }

        }
        itemHolder?.bind(item)
    }

    open fun onCreateView(parent: ViewGroup, viewType: Int): HolderBinder<T>? {
        val cl = binderMap[viewType].second
        val obj = cl.getConstructor(ViewGroup::class.java)
        return obj.newInstance(parent) as? HolderBinder<T>
    }

    fun getThisItem(position: Int): T? {
        val ix = getItemPosition(position)
        return if (ix < 0) null else thisFiltered[ix]
    }

    private fun getItemPosition(nPosition: Int): Int {
        val nPos = nPosition - thisHeaders.size
        if (nPos < 0) return -1
        return if (nPos >= thisFiltered.size) -1 else nPos
    }
    fun getItemIndex(item : T) :Int
    {
        var ix = thisFiltered.indexOf(item)
        if (ix >= 0) ix += thisHeaders.size
        return ix
    }

    override fun getItemCount(): Int =
            thisFiltered.size + thisHeaders.size + thisFooters.size

    override fun getItemViewType(position: Int): Int
            = getThisItem(position)?.let { getBindClassType(it) }
            ?: getItemViewTypeInt(position)

    fun getBindClassType(item : T):Int
    {
        onBindTypeAction?.invoke(item)?.apply {
            val ix = binderMap.indexOfFirst { it.second == this }
            if (ix >= 0) return ix
            bindClass(item.javaClass, this)
        }
        val ix = binderMap.indexOfFirst { it.first == item.javaClass }
        return if (ix < 0) 0 else ix
    }

    private fun getItemViewTypeInt(position: Int): Int {
        if (position < thisHeaders.size) return TYPE_HEADER
        if (position - thisHeaders.size - thisFiltered.size >= 0) return TYPE_FOOTER
        return TYPE_ITEM
    }

    fun clear() {
        refItems = null
        thisFiltered.clear()
        thisHeaders.clear()
        thisFooters.clear()
        selected.clear()
        notifyDataSetChanged()
    }

    fun addHeaderView(v: View) {
        thisHeaders.add(v)
        val vg = v.parent as? ViewGroup?
        vg?.removeView(v)
    }

    fun addFooterView(v: View) {
        thisFooters.add(v)
        val vg = v.parent as? ViewGroup?
        vg?.removeView(v)
    }
    private fun onItemSelect(holder: HolderBinder<T>, item: T)
    {
        if (nSelectMode == SELECT_MODE_NONE) return
        if (selected.contains(item)) {
            selected.remove(item)
            thisHoldersSelected.remove(holder)
            holder.onSelect(item, false)
        } else {
            if (nSelectMode == SELECT_MODE_SINGLE) {
                for (view in thisHoldersSelected) {
                    view.onSelect(item, false)
                }
                selected.clear()
                thisHoldersSelected.clear()
            }
            selected.add(item)
            thisHoldersSelected.add(holder)
            holder.onSelect(item, true)
        }
    }
    /**
     * Fibrillated
     */
    private fun fAdd(position : Int, items : List<T>)
    {
        thisFiltered.addAll(position, items)
        notifyItemRangeInserted(position+thisHeaders.size, items.size)
    }
    private fun fReplace(position: Int, items : List<T>){
        repeat(items.size){
            thisFiltered[position+it] = items[it]
        }
        notifyItemRangeChanged(position+thisHeaders.size, items.size)
    }
    private fun fRemove(position: Int, count : Int){

        repeat(count){
            thisFiltered.removeAt(position)
        }
        notifyItemRangeRemoved(position+thisHeaders.size, count)
    }
    private fun fFilter(items: List<T>): ArrayList<T>
    {
        val newItems = ArrayList<T>()
        items.apply {
            itemFilterHandler?.apply {
                forEach { if (invoke(it)) newItems.add(it) }
            }?: newItems.addAll(this)
        }
        return newItems
    }
    /**
     * Refresh items
     */
    fun refresh(index:Int)
    {
        notifyItemChanged(index)
    }
    fun refresh()
    {
        val newItems = fFilter(thisItems)
        var oldIx = 0
        var newIx = 0

        while(newIx < newItems.size && oldIx < thisFiltered.size)
        {
            if (thisFiltered[oldIx] == newItems[newIx]){
                oldIx++
                newIx++
                continue
            }
            //  Найти похожие объекты
            val oldStart = oldIx
            while(newIx < newItems.size && oldIx < thisFiltered.size)
            {
                if (thisFiltered[oldIx] == newItems[newIx]) break
                oldIx++
            }

            fRemove(oldStart, oldIx - oldStart)
            oldIx = oldStart
        }

        if (oldIx < newItems.size) {
            val sz = newItems.size - oldIx
            fAdd(oldIx, newItems.subList(newIx, newIx+sz))
            oldIx += sz
            newIx += sz
        }
        if (thisFiltered.size > newItems.size){
            fRemove(newIx,  thisFiltered.size - newItems.size)
        }
    }
    fun refreshAll()
    {
        val newItems = fFilter(thisItems)
        val maxChange = Math.min(thisFiltered.size, newItems.size)
        fReplace(0, newItems.subList(0, maxChange))

        val diff = thisFiltered.size - newItems.size
        when {
            diff > 0 -> fRemove(newItems.size, diff)
            diff < 0 -> fAdd(thisFiltered.size,
                            newItems.subList(thisFiltered.size, newItems.size))
        }
    }
    /**
     * Manipulates
     */
    fun insertItems(insertItems: List<T>, position: Int)
    {
        val nPos = Math.min(position, thisItems.size)
        thisItems.addAll(nPos, insertItems)

        val newItems = fFilter(thisItems)

        var oldIx = 0
        var newIx = 0
        while(newIx < newItems.size && oldIx < thisFiltered.size)
        {
            if (newItems[newIx] == thisFiltered[oldIx]){
                oldIx++
                newIx++
                continue
            }
            val newStart = newIx
            while(newIx < newItems.size && oldIx < thisFiltered.size)
            {
                if (thisFiltered[oldIx] == newItems[newIx]) break
                newIx++
            }
            fAdd(oldIx, newItems.subList(newStart, newIx))
            oldIx += newIx - newStart
        }
        if (newItems.size > thisFiltered.size)
            fAdd(oldIx, newItems.subList(newIx, newItems.size))
    }
    fun removeItem(position: Int, count : Int)
    {
        val nSize =  Math.min(thisItems.size, position + count) - position
        if (nSize < 0) return

        for(i in 0 until nSize) thisItems.removeAt(position)
        refresh()
    }
    fun replaceItem(item : T, action : ((T) -> Boolean)? = null) : Boolean
    {
        var bReplaced = false

        if (action == null) {
            thisItems.forEachIndexed { index, t ->
                if (t == item){
                    bReplaced = true
                    thisItems[index] = item
                }
            }
            thisFiltered.forEachIndexed { index, t ->
                if (t == item){
                    bReplaced = true
                    fReplace(index, listOf(item))
                }
            }
        }else{
            thisItems.forEachIndexed { index, t ->
                if (action(t)){
                    bReplaced = true
                    thisItems[index] = item
                }
            }
            thisFiltered.forEachIndexed { index, t ->
                if (action(t)){
                    bReplaced = true
                    fReplace(index, listOf(item))
                }
            }
        }
        return bReplaced
    }

    companion object {
        private val TYPE_ITEM = 0
        private val TYPE_HEADER = -1
        private val TYPE_FOOTER = -2

        val SELECT_MODE_NONE = 0
        val SELECT_MODE_SINGLE = 1
        val SELECT_MODE_MULTIPLY = 2
    }
}

