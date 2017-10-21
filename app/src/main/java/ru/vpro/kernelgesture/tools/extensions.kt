package org.inowave.planning.common.tools

import android.content.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import ru.vpro.kernelgesture.BuildConfig
import ru.vpro.kernelgesture.tools.adapter.RecyclerAdapterT
import java.util.*

fun EditText.bindToSubject(rxSubject: Subject<String>, composites: CompositeDisposable):EditText
{
    composites += rxSubject
        .filter { editableText.toString() != it }
        .subscribe{
            setText(it)
            setSelection(length())
        }
    addTextChangedListener(object:TextWatcher{
        override fun afterTextChanged(p0: Editable?) { }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            rxSubject.onNext(p0.toString())
        }
    })
    return this
}
fun EditText.onChange(value : String? = null, action : (String) -> Unit):EditText
{
    setText(value)
    addTextChangedListener(object:TextWatcher{
        override fun afterTextChanged(p0: Editable?) { }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int)
        {
            if (value != p0.toString()) {
                action(p0.toString())
            }
        }
    })
    return this
}


fun Switch.bindToSubject(rxSubject: Subject<Boolean>, composites : CompositeDisposable)
{
    setOnCheckedChangeListener { compoundButton, bValue ->
        rxSubject.onNext(bValue)
    }
    composites += rxSubject
        .filter { isChecked != it }
        .subscribe{ isChecked = it }
}

fun RecyclerView.bindAdapter(adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>)
{
    setHasFixedSize(true)

    layoutManager = LinearLayoutManager(context)
    addItemDecoration(RecyclerAdapterT.AdapterDivider(context))
    (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    this.adapter = adapter
}

fun Fragment.runOnUiThread(action: () -> Unit)
{
    activity?.runOnUiThread(action)
}

fun <T>Random.nextItem(items : List<T>):T
{
    return items[nextInt(items.size)]
}
fun Random.nextDay(bound : Long) : Long
    = (nextDouble()*bound*24*60*60*1000).toLong()

fun snackbarCustom(view: View?, nLayoutRes : Int,
                   initAction : ((snackbar : Snackbar, view : View)->Unit)? = null) : Snackbar?
{
    if (view == null) return null
    val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)

    (snackbar.view as Snackbar.SnackbarLayout).apply {

        findViewById<TextView>(android.support.design.R.id.snackbar_text)
                ?.visibility = View.INVISIBLE

        setPadding(0,0,0,0)
        background = null

        val snackView = LayoutInflater.from(view.context)
                .inflate(nLayoutRes, this)

        initAction?.invoke(snackbar, snackView)
    }

    snackbar.show()
    return snackbar
}

fun Snackbar.onDismissed(action : ()->Unit)
{
    addCallback(object : Snackbar.Callback(){
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            super.onDismissed(transientBottomBar, event)
            action()
        }
    })
}

fun RecyclerView.onScrolled(action : (recyclerView: RecyclerView?, dx: Int, dy: Int)->Unit)
{
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            action(recyclerView, dx, dy)
        }
    })
}
fun RecyclerView.onScrollStateChanged(action : (recyclerView: RecyclerView?, newState: Int)->Unit)
{
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            action(recyclerView, newState)
        }
    })
}

fun BuildConfigVersion()
        = "ver: ${BuildConfig.VERSION_NAME}"

fun ViewPager.currentFragment(fragment : Fragment)
        = fragment.childFragmentManager.findFragmentByTag("android:switcher:" + id + ":" + currentItem)

fun ViewPager.onPageChangeListener(function: (position: Int) -> Unit)
{
    addOnPageChangeListener(object : ViewPager.OnPageChangeListener
    {
        override fun onPageScrollStateChanged(state: Int) {}
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        override fun onPageSelected(position: Int) {
            function(position)
        }
    })
}

fun Context.toUri(drawableId : Int)
    = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
        "://" + resources?.getResourcePackageName(drawableId)
        + '/' + resources?.getResourceTypeName(drawableId)
        + '/' + resources?.getResourceEntryName(drawableId))

/**
 * Асевдо MenuItem для опроса действий
 */
fun makeMenuItem(menuItemId: Int): MenuItem
{
    return object : MenuItem {
        override fun getItemId(): Int = menuItemId
        override fun expandActionView(): Boolean = false
        override fun hasSubMenu(): Boolean = false
        override fun getMenuInfo(): ContextMenu.ContextMenuInfo {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun getAlphabeticShortcut(): Char = ' '
        override fun setEnabled(p0: Boolean): MenuItem = this
        override fun setTitle(p0: CharSequence?): MenuItem = this
        override fun setTitle(p0: Int): MenuItem = this
        override fun setChecked(p0: Boolean): MenuItem = this
        override fun getActionView(): View {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun getTitle(): CharSequence = ""
        override fun getOrder(): Int = 0
        override fun setOnActionExpandListener(p0: MenuItem.OnActionExpandListener?): MenuItem = this
        override fun getIntent(): Intent = Intent()
        override fun setVisible(p0: Boolean): MenuItem = this
        override fun isEnabled(): Boolean = true
        override fun isCheckable(): Boolean = false
        override fun setShowAsAction(p0: Int) {}
        override fun getGroupId(): Int = 0
        override fun setActionProvider(p0: ActionProvider?): MenuItem = this
        override fun setTitleCondensed(p0: CharSequence?): MenuItem = this
        override fun getNumericShortcut(): Char = ' '
        override fun isActionViewExpanded(): Boolean = false
        override fun collapseActionView(): Boolean = false
        override fun isVisible(): Boolean = true
        override fun setNumericShortcut(p0: Char): MenuItem = this
        override fun setActionView(p0: View?): MenuItem = this
        override fun setActionView(p0: Int): MenuItem = this
        override fun setAlphabeticShortcut(p0: Char): MenuItem = this
        override fun setIcon(p0: Drawable?): MenuItem = this
        override fun setIcon(p0: Int): MenuItem = this
        override fun isChecked(): Boolean = false
        override fun setIntent(p0: Intent?): MenuItem = this
        override fun setShortcut(p0: Char, p1: Char): MenuItem = this
        override fun getIcon(): Drawable {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun setShowAsActionFlags(p0: Int): MenuItem = this
        override fun setOnMenuItemClickListener(p0: MenuItem.OnMenuItemClickListener?): MenuItem = this
        override fun getActionProvider(): ActionProvider {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun setCheckable(p0: Boolean): MenuItem = this
        override fun getSubMenu(): SubMenu {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun getTitleCondensed(): CharSequence {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}

fun Context.toClipboard(value : String){
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip =  ClipData.newPlainText("", value)
}

fun <T>SharedPreferences.bindToSubject(subject : BehaviorSubject<T>,
                                       key : String, defaultValue : T,
                                       composites : CompositeDisposable)
{
    when(defaultValue){
        is Boolean  -> getBoolean(key, defaultValue) as T
        is String   -> getString(key, defaultValue) as T
        is Int      -> getInt(key, defaultValue) as T
        is Long     -> getLong(key, defaultValue) as T
        is Float    -> getFloat(key, defaultValue) as T
        else -> null
    }?.let{
        if (subject.value != it)
            subject.onNext(it)
    }

    composites += subject.subscribe {

        edit().apply {
            when(it){
                is Boolean  -> putBoolean(key, it)
                is String   -> putString(key, it)
                is Int      -> putInt(key, it)
                is Long     -> putLong(key, it)
                is Float    -> putFloat(key, it)
            }
        }.apply()
    }
}
