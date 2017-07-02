package ru.vpro.kernelgesture

import android.content.Context
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.widget.Switch
import android.view.View
import android.widget.CompoundButton

class GestureSwitchPreference : SwitchPreference
{
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context:Context, attrs:AttributeSet, defStyleAttr:Int) : super(context, attrs, defStyleAttr)
    constructor(context:Context, attrs:AttributeSet, defStyleAttr:Int, defStyleRes:Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        widgetLayoutResource = R.layout.preference_switch_item
    }

    override fun onBindView(view: View)
    {
        super.onBindView(view)

        view.setOnClickListener{
            onPreferenceClickListener?.onPreferenceClick(this)
        }

        val checkableView:Switch? = view.findViewById(R.id.switch_widget) as Switch?
        checkableView?.setOnCheckedChangeListener({ compoundButton: CompoundButton, b: Boolean ->

            if (!callChangeListener(b)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                checkableView.isChecked = !b
            }else {
                isChecked = b
            }

        })
        checkableView?.isChecked = isChecked
    }
}