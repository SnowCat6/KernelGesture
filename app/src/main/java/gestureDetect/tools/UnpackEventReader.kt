package gestureDetect.tools

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream


class UnpackEventReader(val context: Context)
{
    fun copyResourceTo(localSource: String, destinationFile: String)
            : String?
    {
        val applicationInfo = try {
            context.packageManager.getApplicationInfo(context.packageName, 0)
        } catch (x: Throwable) { return null }

        val destName = applicationInfo.dataDir + "/" + destinationFile

        try {
            val zipFile = applicationInfo.publicSourceDir
            val fin = FileInputStream(zipFile)
            val zin = ZipInputStream(fin)

            while (true) {
                val ze = zin.nextEntry ?: break
                if (ze.name != localSource) continue

                val fout = FileOutputStream(destName, false)
                val buffer = ByteArray(8192)

                while (true) {
                    val len = zin.read(buffer)
                    if (len <= 0) break
                    fout.write(buffer, 0, len)
                }
                fout.close()
                zin.closeEntry()

                return destName
            }
        }catch (e:Exception){
            e.printStackTrace()

            if (File(destName).exists())
                return destName
        }
        return null
    }
}