package gestureDetect.drivers

import android.graphics.Point
import android.util.Log
import gestureDetect.GestureDetect
import gestureDetect.drivers.input.*
import gestureDetect.tools.GestureHW
import gestureDetect.tools.UnpackEventReader
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import ru.vpro.kernelgesture.BuildConfig
import java.io.BufferedReader
import java.io.FileReader
import kotlin.concurrent.thread

/**
 * Класс для получения событий мз устройств ввода, /dev/input/
 */

class SensorInput(gesture: GestureDetect): SensorHandler(gesture)
{
    private var bRunning = false
    private var runThread:Thread? = null

    /**
     * Input devices
     */
    private val inputHandlers = arrayOf(
            InputTouchscreen(gesture),
            InputQCOMM_KPD(gesture),
            InputSunXi_KPD(gesture),
            InputCypressTPD(gesture)
    )
    data class EvData(
            val evName:String,
            val evButton:String,
            val evPress:String,
            val evMilliTime:Double,
            val coordinates:Point
    )

    private var inputDevices = emptyList<Pair<String, InputHandler>>()
    private val composites = CompositeDisposable()

    override fun enable(bEnable: Boolean)
            = inputDevices.forEach { it.second.setEnable(bEnable) }

    override fun close()
    {
        composites.clear()
        bRunning = false

        if (runThread?.isAlive == true) {
            //  Many execute for flush process buffer
            for (ix in 0..15) gesture.su.exec("echo CLOSE_EVENTS>&2")
        }
    }

    override fun onCreate()
    {
        if (composites.size() != 0) return

        composites += gesture.su.su.rxRootEnable
                .filter { it }
                .subscribe {

                    inputDevices = emptyList()
                    getInputEvents().forEach { (input, name) ->
                        inputHandlers.forEach {
                            if (it.onDetect(name)) {
                                if (BuildConfig.DEBUG) {
                                    Log.d("SensorInput", "device $name => $input")
                                }
                                inputDevices += Pair(input, it)
                            }
                        }
                    }

                    startThread()
                    inputDevices.isNotEmpty()
                }

        GestureHW.rxScreenOn
                .filter { it }
                .observeOn(Schedulers.io())
                .subscribe {
                    enable(true)
                }
    }

    /**
     * Exec command for get events from getevent linux binary
     */
    private fun evCmd(queryIx:Long, ix:Int, device:Pair<String, InputHandler>, nLimit:Int, nRepeat:Int)
    {
        if (nLimit > 0) {
            val CR = "\\\\n"
            val seq = (1..nRepeat).joinToString(" ")
            gesture.su.exec("while true ; do v$ix=\$(getevent -c $nLimit -tl ${device.first} | grep ${device.second.rawFilter}) ; [ \"\$v$ix\" ] && for i in $seq ; do echo query$queryIx$CR${device.first}$CR\"\$v$ix\">&2 ; done ; done &")
        }else{
            gesture.su.exec("getevent -tl ${device.first}>&2 &")
        }
    }

    override fun onResume()
    {
        bRunning = true
        startThread()
    }

    private fun startThread()
    {
        if (runThread?.isAlive == true || !bRunning)
            return

        if (!gesture.su.checkRootAccess() || inputDevices.isEmpty())
            return

        runThread = thread(priority = Thread.MAX_PRIORITY){

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Start")
            }

            while(bRunning){
                if (!gesture.su.checkRootAccess()) break
                val reader = UnpackEventReader(context)
                val cmdName = reader.copyResourceTo(
                        "lib/armeabi-v7a/EventReader.so",
                        "EventReader")

                if (cmdName != null && gesture.su.isFileExists(cmdName)){
                    gesture.su.exec("chmod 777 $cmdName")
                    threadLoopNew(cmdName)
                }else {
                    threadLoop()
                }
            }
            runThread = null

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Exit")
            }
        }
    }

    private fun threadLoopNew(cmdName: String)
    {
        val arg = inputDevices.joinToString(" ") { it.first }
        gesture.su.exec("$cmdName $arg&")

        val regSplit = Regex("\\[\\s*([^\\s]+)\\]\\s*([^\\s]+):\\s+([^\\s]+)\\s+([^\\s]+)\\s*([^\\s]+)")
        val coordinates = Point(-1, -1)

        while(bRunning) {
            //  Read line from input
            val rawLine = gesture.su.readErrorLine() ?: break
            //  Stop if gesture need stop run
            if (!bRunning) break
            //  Check device is near screen
            if (gesture.isNearProximity) continue
            val ev = regSplit.find(rawLine) ?: continue

            val timeout = ev.groupValues[1].toDoubleOrNull()?:continue

            if (ev.groupValues[3] != "EV_KEY") continue
            if (ev.groupValues[5] != "DOWN") continue
            val evInput = EvData(ev.groupValues[2],
                    ev.groupValues[4],
                    ev.groupValues[5],
                    timeout, coordinates)

            inputDevices
                    .find { it.first == ev.groupValues[2] }
                    ?.second?.onEvent(evInput)
                    ?.apply { sensorEvent(this) }
        }
    }

    private fun threadLoop()
    {
        var device = ""
        var bQueryFound = false
        val queryIx = System.currentTimeMillis()

        //  For each device
        var ix = 0
        inputDevices.forEach {

            //  Run input event detector
            evCmd(queryIx, ++ix, it, 2, 4)
            evCmd(queryIx, ++ix, it, 4, 2)
        }

        val regSplit = Regex("\\[\\s*([^\\s]+)\\]\\s*([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)")
        val coordinates = Point(-1, -1)
        var lastEventTime = 0.0
        var eqEvents = emptyList<String>()

        while(bRunning)
        {
            //  Read line from input
            val rawLine = gesture.su.readErrorLine() ?: break
            //  Stop if gesture need stop run
            if (!bRunning) break

            //  Check query number for skip old events output

            if (!bQueryFound){
                bQueryFound = rawLine == "query$queryIx"
                continue
            }

            //  Check device is near screen
            if (gesture.isNearProximity) continue
            if (eqEvents.contains(rawLine)) continue

            if (rawLine.contains("/dev/input/")){
                device = rawLine
                continue
            }

            val ev = regSplit.find(rawLine) ?: continue
            eqEvents += rawLine

            val timeLine = ev.groupValues[1].toDoubleOrNull()?:continue
            var timeout = timeLine - lastEventTime

            if (timeout < -1000.0) timeout = 1.0
            if (timeout < 0) continue
            if (timeout > 0) eqEvents = listOf(rawLine)
            lastEventTime = timeLine

            //  Check only key events
            if (ev.groupValues[3] == "ABS_MT_POSITION_X"){
                coordinates.x = ev.groupValues[4].toInt(16)
                continue
            }
            if (ev.groupValues[3] == "ABS_MT_POSITION_Y"){
                coordinates.y = ev.groupValues[4].toInt(16)
                continue
            }

            if (ev.groupValues[2] != "EV_KEY") continue
//                if (ev.groupValues[4] != "DOWN") continue
            val evInput = EvData(ev.groupValues[2],
                    ev.groupValues[3],
                    ev.groupValues[4],
                    lastEventTime, coordinates)

            inputDevices
                    .find { it.first == device }
                    ?.second?.onEvent(evInput)
                    ?.apply { sensorEvent(this) }
        }
        gesture.su.killJobs()
    }

    companion object
    {
        fun getInputEvents():List<Pair<String, String>>
        {
            var inputs = emptyList<Pair<String, String>>()
            val regName = Regex("Name=\"(.*)\"")
            val regHandlers = Regex("Handlers=(.*)")

            try {
                var name = ""
                BufferedReader(FileReader("/proc/bus/input/devices"))
                        .forEachLine { line ->

                            regName.find(line)?.apply {
                                name = groupValues[1]
                            } ?:
                            regHandlers.find(line)?.apply {
                                groupValues[1]
                                        .split(" ")
                                        .filter { it.startsWith("event") }
                                        .forEach { inputs += Pair("/dev/input/$it", name) }
                            }
                        }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return inputs
        }
    }
}
