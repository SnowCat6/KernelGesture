package gestureDetect.drivers

import android.graphics.Point
import android.util.Log
import gestureDetect.GestureDetect
import gestureDetect.drivers.input.*
import ru.vpro.kernelgesture.BuildConfig
import java.io.BufferedReader
import java.io.FileReader
import kotlin.concurrent.thread

/**
 * Класс для получения событий мз устройств ввода, /dev/input/
 */

class SensorInput(gesture: GestureDetect): SensorHandler(gesture)
{
    var bRunning = false
    var bRunThread = false

    /**
     * Input devices
     */
    private val inputHandlers = arrayOf(
            InputTouchscreen(gesture),
            InputMTK_KPD(gesture),
            InputQCOMM_KPD(gesture),
            InputSunXi_KPD(gesture)
    )
    data class EvData(
            val evName:String,
            val evButton:String,
            val evPress:String,
            val evMilliTime:Double,
            val coordinates:Point
    )

    private var inputDevices = emptyList<Pair<String, InputHandler>>()

    override fun enable(bEnable: Boolean)
            = inputDevices.forEach { it.second.setEnable(bEnable) }

    override fun close()
    {
        bRunning = false

        if (bRunThread) {
            bRunThread = false

//            gesture.su.exec("kill -s SIGINT %%")
            gesture.su.exec("kill -s SIGINT \$(jobs -p)")
            //  Many execute for flush process buffer
            for (ix in 0..15) gesture.su.exec("echo CLOSE_EVENTS>&2")
            Thread.sleep(1*1000)
        }
    }

    override fun onDetect(): Boolean
    {
        inputDevices = emptyList()

        if (!gesture.su.hasRootProcess()){
            close()
            return false
        }

        getInputEvents().forEach { (input, name) ->
            inputHandlers.forEach {
                if (it.onDetect(name)) inputDevices += Pair(input, it)
            }
        }

        startThread()

        return inputDevices.isNotEmpty()
    }

    /**
     * Exec command for get events from getevent linux binary
     */
    private fun evCmd(queryIx:Long, ix:Int, device:Pair<String, InputHandler>, nLimit:Int, nRepeat:Int){
        val CR = "\\\\n"
        val seq =(1 .. nRepeat).joinToString(" ")
        gesture.su.exec("while true ; do v$ix=\$(getevent -c $nLimit -tl ${device.first} | grep ${device.second.rawFilter}) ; [ \"\$v$ix\" ] && for i in $seq ; do echo query$queryIx$CR${device.first}$CR\"\$v$ix\">&2 ; done ; done &")
    }

    override fun onStart()
    {
        bRunning = true
        if (gesture.su.checkRootAccess(context) && inputDevices.isNotEmpty())
            startThread()
    }

    override fun onStop() {
        bRunning = false
    }

    private fun startThread()
    {
        if (bRunThread || !bRunning) return
        bRunThread = true

        thread(priority = Thread.MAX_PRIORITY){

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Start")
            }

            var device = ""
            var bQueryFound = false
            val queryIx = System.currentTimeMillis()

            //  For each device
            var ix = 0
            inputDevices.forEach {

                //  Run input event detector
                evCmd(queryIx, ++ix, it, 2, 5)
                evCmd(queryIx, ++ix, it, 4, 2)
            }

            var coordinates = Point(-1, -1)
            var lastEventTime = 0.0
            var eqEvents = emptyList<String>()
            val regSplit = Regex("\\[\\s*([^\\s]+)\\]\\s*([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)")

            while(bRunThread)
            {
                //  Read line from input
                val rawLine = gesture.su.readErrorLine() ?: break
                //  Stop if gesture need stop run
                if (!bRunThread) break

                //  Check query number for skip old events output
                if (!bQueryFound){
                    bQueryFound = rawLine == "query$queryIx"
                    continue
                }

                if (rawLine == "CLOSE_EVENTS")
                    break

                if (gesture.disabled) continue
                //  Check device is near screen
                if (gesture.isNearProximity) continue
                if (eqEvents.contains(rawLine)) continue
                eqEvents += rawLine

                val ev = regSplit.find(rawLine)
                if (ev == null){
                    //  Detect current input device
                    if (rawLine.contains("/dev/input/")) device = rawLine
                    continue
                }
/*
                Log.d("Sensor", device)
                inputDevices.forEach {
                    Log.d("Sensors", it.first)
                }
*/
                val timeLine = ev.groupValues[1].toDoubleOrNull() ?: continue
                val timeout = timeLine - lastEventTime
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
                        timeLine, coordinates)
                coordinates = Point(-1, -1)

                inputDevices
                        .find { it.first == device }
                        ?.second?.onEvent(evInput)
                        ?.apply { sensorEvent(this) }
            }
            bRunThread = false

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Exit")
            }
        }.priority = Thread.MAX_PRIORITY
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
                            }
                            regHandlers.find(line)?.apply {
                                groupValues[1].split(" ")
                                        .filter { it.contains("event") }
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
