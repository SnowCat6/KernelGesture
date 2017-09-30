package gestureDetect.tools

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import io.reactivex.subjects.PublishSubject
import ru.vpro.kernelgesture.BuildConfig

class GestureSettings(val context: Context)
{
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getAllEnable(): Boolean
            = getEnable(GESTURE_ENABLE)

    fun setAllEnable(value: Boolean) {
        setEnable(GESTURE_ENABLE, value)
    }
    fun getEnable(key: String?): Boolean
    {
        if (key == null) return false
        val ret = sharedPreferences.getBoolean(key, false)
        if (BuildConfig.DEBUG) {
            Log.d("getEnable", "$key=$ret")
        }
        return ret
    }
    fun setEnable(key: String, value: Boolean)
    {
        val e = sharedPreferences.edit()
        e.putBoolean(key, value)
        e.apply()

        rxUpdateValue.onNext(PreferenceChange(key, value))
    }

    fun getAction(key: String): String? {

        try {
            return sharedPreferences.getString("${key}_ACTION", null)
        } catch (e: Exception) { }
        return null
    }

    fun setAction(key: String, value: String?)
    {
        val e = sharedPreferences.edit()
        if (value == null){
            e.remove(key)
        }else {
            e.putString("${key}_ACTION", value)
        }
        e.apply()

        rxUpdateValue.onNext(PreferenceChange(key, value))
    }

    companion object {
        val GESTURE_ENABLE = "GESTURE_ENABLE"
        data class PreferenceChange(val key : String, val value : Any?)
        val rxUpdateValue = PublishSubject.create<PreferenceChange>()
    }
}