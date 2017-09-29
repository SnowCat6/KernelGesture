package ru.vpro.kernelgesture.tools

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.adapter_switch_item.view.*
import ru.vpro.kernelgesture.R

class GestureSwitchPreference
    : SwitchPreference
{
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    init{
        widgetLayoutResource = R.layout.adapter_switch_item
    }

    override fun onBindView(view: View)
    {
        super.onBindView(view)

        view.setOnClickListener{
            onPreferenceClickListener?.onPreferenceClick(this)
        }

        view.switch_widget?.setOnCheckedChangeListener{ compoundButton: CompoundButton, b: Boolean ->

            isChecked = if (!callChangeListener(b)) !b else b
        }
        view.switch_widget?.isChecked = isChecked
    }
}