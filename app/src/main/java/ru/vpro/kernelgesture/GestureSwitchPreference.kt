package ru.vpro.kernelgesture

import android.content.Context
import android.preference.Preference
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.widget.Switch
import android.view.View


/**
 * Created by Костя on 30.05.2017.
 */
class GestureSwitchPreference(context: Context, attrs: AttributeSet) :
        SwitchPreference(context, attrs)
{

    override fun onBindView(view: View)
    {
        super.onBindView(view)

        view.setOnClickListener{
            onPreferenceClickListener?.onPreferenceClick(this)
        }

        val checkableView:Switch? = view.findViewById(android.R.id.switch_widget) as Switch?
        checkableView?.setOnClickListener {
            if (!callChangeListener(isChecked)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                checkableView.setChecked(!isChecked)
            }else {
                setChecked(isChecked)
            }
        }
    }
}