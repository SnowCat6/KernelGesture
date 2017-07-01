package gesture.action

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Camera
import android.util.Log
import gesture.GestureAction
import gesture.GestureDetect
import ru.vpro.kernelgesture.R

class ActionFlashlight(action: GestureAction) : ActionItem(action)
{
    val devices = arrayOf(
            "/sys/class/leds/flashlight/brightness"
    )

    override fun action(): String {
        return if (bHasFlash) "application.flashlight" else ""
    }

    override fun name(): String
            = action.context.getString(R.string.ui_flashlight)

    override fun icon(): Drawable
            = action.context.getDrawable(R.drawable.icon_flashlight)

    override fun run(): Boolean
    {
        action.vibrate()
        action.playNotify()

        enable = !enable
        return false
    }

    override fun onStop() {
        enable = false
        closeCamera()
    }

    companion object {
        var bIsDetected = false
        var flashlightDirect:String? = null
        var bEnable = false
        var bHasFlash = false
    }
    var camera:Camera? = null
    var params: Camera.Parameters? = null

    var enable:Boolean
        get() = bEnable
        set(value) {
            bEnable = value
            if (flashlightDirect != null) flashlightDirect()
            else flashlightCamera()
        }

    init{
        if (!bIsDetected && GestureDetect.SU.hasRootProcess())
        {
            for (it in devices) {
                if (!GestureDetect.SU.isFileExists(it)) continue
                    flashlightDirect = it
                    bIsDetected = true
                break
            }
        }
        if (!bIsDetected){
            if (action.context.applicationContext.packageManager
                    .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            {
                try{
                    camera = Camera.open()
                    bHasFlash = true
                    camera?.release()
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        bIsDetected = true
    }

    fun flashlightDirect(){
        GestureDetect.SU.exec("echo ${if (bEnable) 255 else 0} > ${flashlightDirect}" )
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
            params?.flashMode = Camera.Parameters.FLASH_MODE_TORCH;
            camera?.parameters = params;
            camera?.startPreview();
        }else{
            params?.flashMode = Camera.Parameters.FLASH_MODE_OFF;
            camera?.parameters = params;
            camera?.stopPreview();
        }
    }
    fun closeCamera(){
        camera?.release()
        camera = null
        params = null
    }
}