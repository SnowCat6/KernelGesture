package gestureDetect.tools

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.reactivex.subjects.PublishSubject

class GestureSettings(val context: Context)
{
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context) as SharedPreferences

    fun getAllEnable(): Boolean
            = getEnable(GESTURE_ENABLE)

    fun setAllEnable(value: Boolean) {
        setEnable(GESTURE_ENABLE, value)
    }
    fun getEnable(key: String?): Boolean
    {
        if (key == null) return false
        return sharedPreferences.getBoolean(key, false)
    }
    fun setEnable(key: String, value: Boolean)
    {
        val e = sharedPreferences.edit()
        e.putBoolean(key, value)
        e.apply()

        rxUpdateValue.onNext(PreferenceChange(key, value))
//        val intent = Intent(EVENT_ENABLE)
//        intent.putExtra("key", key as Serializable)
//        intent.putExtra("value", value as Serializable)
//        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
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
        //        val EVENT_ENABLE = "EVENT_ENABLE"
        data class PreferenceChange(val key : String, val value : Any?)
        val rxUpdateValue = PublishSubject.create<PreferenceChange>()
    }
}