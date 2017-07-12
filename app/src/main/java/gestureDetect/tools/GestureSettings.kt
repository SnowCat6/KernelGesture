package gestureDetect.tools

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import java.io.Serializable

class GestureSettings(val context: Context)
{
    companion object {
        val EVENT_ENABLE = "EVENT_ENABLE"
    }
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context) as SharedPreferences

    fun getAllEnable(): Boolean
            = getEnable("GESTURE_ENABLE")

    fun setAllEnable(value: Boolean) {
        setEnable( "GESTURE_ENABLE", value)
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

        val intent = Intent(EVENT_ENABLE)
        intent.putExtra("key", key as Serializable)
        intent.putExtra("value", value as Serializable)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
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
    }
}