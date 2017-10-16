package gestureDetect.tools

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class UnpackEventReader(val context: Context)
{
    fun copyResourceTo(sourceName: String, destinationName: String)
            : String?
    {
        val source = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.forEach {
                source.add("lib/$it/$sourceName")
            }
        } else {
            source.add("lib/${Build.CPU_ABI}/$sourceName")
        }

        val applicationInfo = try {
            context.packageManager.getApplicationInfo(context.packageName, 0)
        } catch (x: Throwable) { return null }

        val destName = applicationInfo.dataDir + "/" + destinationName
        try{
            File(destName).delete()
        }catch (e:Exception){}

        try {
            val zipFile = applicationInfo.publicSourceDir
            val fin = FileInputStream(zipFile)

            source.forEach {
                getZipFile(ZipInputStream(fin), it)?.also {

                    val fout = FileOutputStream(destName, false)
                    val buffer = ByteArray(8192)

                    while (true) {
                        val len = it.read(buffer)
                        if (len <= 0) break
                        fout.write(buffer, 0, len)
                    }
                    fout.close()
                    it.closeEntry()

                    File(destName).deleteOnExit()
                    return destName
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
            if (File(destName).exists())
                return destName
        }
        return null
    }

    private fun getZipFile(zin: ZipInputStream, abi: String) : ZipInputStream?
    {
        while (true) {
            val ze = zin.nextEntry ?: break
            if (abi != ze.name) continue
            return zin
        }
        zin.close()
        return null
    }
}