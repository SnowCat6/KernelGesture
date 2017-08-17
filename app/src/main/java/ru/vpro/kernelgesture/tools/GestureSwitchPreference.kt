package ru.vpro.kernelgesture.tools

import android.content.Context
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.adapter_switch_item.view.*
import ru.vpro.kernelgesture.R

class GestureSwitchPreference : SwitchPreference
{
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context:Context, attrs:AttributeSet, defStyleAttr:Int) : super(context, attrs, defStyleAttr)
    constructor(context:Context, attrs:AttributeSet, defStyleAttr:Int, defStyleRes:Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        widgetLayoutResource = R.layout.adapter_switch_item
    }

    override fun onBindView(view: View)
    {
        super.onBindView(view)

        view.setOnClickListener{
            onPreferenceClickListener?.onPreferenceClick(this)
        }

        view.switch_widget?.setOnCheckedChangeListener({ compoundButton: CompoundButton, b: Boolean ->

            if (!callChangeListener(b)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                isChecked = !b
            }else {
                isChecked = b
            }

        })
        view.switch_widget?.isChecked = isChecked
    }
}