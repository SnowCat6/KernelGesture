package gestureDetect.action

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Camera
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.R

class ActionFlashlight(action: GestureAction) : ActionItem(action)
{
    val devices = arrayOf(
            "/sys/class/leds/flashlight/brightness"
    )
    override fun onDetect():Boolean
    {
        bHasFlash = false

        if (action.su.hasRootProcess())
        {
            for (it in devices) {
                if (!action.su.isFileExists(it)) continue
                flashlightDirect = it
                bHasFlash = true
                return bHasFlash
            }
        }

        if (action.context.applicationContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            try{
                val camera = Camera.open()
                val params = camera?.parameters
                bHasFlash = true
                camera?.release()
            }catch (e:Exception){
            }
        }
        return bHasFlash
    }

    override fun action(): String {
        return if (bHasFlash) "application.flashlight" else ""
    }

    override fun name(): String
            = context.getString(R.string.ui_action_flashlight)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_flashlight)

    override fun run(): Boolean
    {
        enable = !enable
        action.vibrate()
//        if (enable) action.playNotify()
        return false
    }

    override fun onStop() {
        enable = false
        closeCamera()
    }

    var bHasFlash = false
    var flashlightDirect:String? = null
    var camera:Camera? = null

    var enable:Boolean = false
        get() = field
        set(value) {
            if (field == value) return
            field = value
            if (flashlightDirect != null) flashlightDirect()
            else flashlightCamera()
        }

    private fun flashlightDirect(){
        action.su.exec("echo ${if (enable) 255 else 0} > $flashlightDirect" )
    }
    private fun flashlightCamera()
    {
        if (!bHasFlash) return

        if (camera == null)
        {
            try {
                camera = Camera.open()
            } catch (e: RuntimeException) {
                e.printStackTrace()
                closeCamera()
                return
            }
        }

        try {
            val params = camera?.parameters ?: return
            if (enable) {
                params.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                camera?.parameters = params
                camera?.startPreview()
            } else {
                params.flashMode = Camera.Parameters.FLASH_MODE_OFF
                camera?.parameters = params
                camera?.stopPreview()
            }
        }catch (e:Exception){
        }
    }
    private fun closeCamera(){
        camera?.release()
        camera = null
    }
}