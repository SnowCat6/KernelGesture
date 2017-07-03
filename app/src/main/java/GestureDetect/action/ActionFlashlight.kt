package GestureDetect.action

import SuperSU.ShellSU
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Camera
import GestureDetect.GestureAction
import ru.vpro.kernelgesture.R

class ActionFlashlight(action: GestureAction) : ActionItem(action)
{
    val devices = arrayOf(
            "/sys/class/leds/flashlight/brightness"
    )
    override fun onDetect():Boolean
    {
        bHasFlash = false

        if (su.hasRootProcess())
        {
            for (it in devices) {
                if (!su.isFileExists(it)) continue
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
                bHasFlash = true
                camera?.release()
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        return bHasFlash
    }

    override fun action(): String {
        return if (bHasFlash) "application.flashlight" else ""
    }

    override fun name(): String
            = context.getString(R.string.ui_flashlight)

    override fun icon(): Drawable
            = context.getDrawable(R.drawable.icon_flashlight)

    override fun run(): Boolean
    {
        enable = !enable
        action.vibrate()
        if (enable) action.playNotify()
        return false
    }

    override fun onStop() {
        enable = false
        closeCamera()
    }


    var flashlightDirect:String? = null
    var bEnable = false
    var bHasFlash = false

    var camera:Camera? = null
    var params: Camera.Parameters? = null
    val su = ShellSU()

    var enable:Boolean
        get() = bEnable
        set(value) {
            bEnable = value
            if (flashlightDirect != null) flashlightDirect()
            else flashlightCamera()
        }

    fun flashlightDirect(){
        su.exec("echo ${if (bEnable) 255 else 0} > $flashlightDirect" )
    }
    fun flashlightCamera()
    {
        if (!bHasFlash) return

        if (camera == null)
        {
            try {
                camera = Camera.open()
                params = camera?.parameters
            } catch (e: RuntimeException) {
                e.printStackTrace()
                closeCamera()
                return
            }
        }

        if (bEnable) {
            params?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            camera?.parameters = params
            camera?.startPreview()
        }else{
            params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
            camera?.parameters = params
            camera?.stopPreview()
        }
    }
    fun closeCamera(){
        camera?.release()
        camera = null
        params = null
    }
}