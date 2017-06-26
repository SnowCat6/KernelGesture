package gesture

/*
SunXI tablet
 */
open class InputSunXi_KPD : InputHandler
{
    override fun onDetect(name:String):Boolean {
        if (!arrayOf("sun4i-keyboard")
                .contains(name.toLowerCase())) return false

        GestureDetect.SUPPORT.add(arrayOf("KEYS", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN"))
        return true
    }

    override fun onEvent(line: String): String? {
        val arg = line.replace(Regex("\\s+"), " ").split(" ")
        if (arg[0] != "EV_KEY") return null

        val keys = arrayOf(
                Pair("KEY_MENU",     "KEY_VOLUMEUP"),
                Pair("KEY_SEARCH",   "KEY_VOLUMEDOWN"))
        return GestureDetect.GS.runGesture(arg[1], keys)
    }
}