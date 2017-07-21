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
    var runThread:Thread? = null

    /**
     * Input devices
     */
    private val inputHandlers = arrayOf(
            InputTouchscreen(gesture),
            InputMTK_KPD(gesture),
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

    override fun enable(bEnable: Boolean)
            = inputDevices.forEach { it.second.setEnable(bEnable) }

    override fun close()
    {
        bRunning = false

        if (runThread?.isAlive == true) {
            //  Many execute for flush process buffer
            for (ix in 0..15) gesture.su.exec("echo CLOSE_EVENTS>&2")
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
                if (it.onDetect(name)){
                    if (BuildConfig.DEBUG){
                        Log.d("SensorInput", "device $name => $input")
                    }
                    inputDevices += Pair(input, it)
                }
            }
        }

        startThread()

        return inputDevices.isNotEmpty()
    }

    /**
     * Exec command for get events from getevent linux binary
     */
    private fun evCmd(queryIx:Long, ix:Int, device:Pair<String, InputHandler>, nLimit:Int, nRepeat:Int)
    {
        val CR = "\\\\n"
        val seq =(1 .. nRepeat).joinToString(" ")
        gesture.su.exec("while true ; do v$ix=\$(getevent -c $nLimit -tl ${device.first} | grep ${device.second.rawFilter}) ; [ \"\$v$ix\" ] && for i in $seq ; do echo query$queryIx$CR${device.first}$CR\"\$v$ix\">&2 ; done ; done &")
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

        if (!gesture.su.checkRootAccess(context) || inputDevices.isEmpty())
            return

        runThread = thread(priority = Thread.MAX_PRIORITY){

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Start")
            }

            while(bRunning){
                if (!gesture.su.checkRootAccess(context)) break
                threadLoop()
            }
            runThread = null

            if (BuildConfig.DEBUG){
                Log.d("SensorInput", "Exit")
            }
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

        var coordinates = Point(-1, -1)
        var lastEventTime = 0.0
        var eqEvents = emptyList<String>()
        val regSplit = Regex("\\[\\s*([^\\s]+)\\]\\s*([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)")

        while(bRunning)
        {
            //  Read line from input
            val rawLine = gesture.su.readErrorLine() ?: break
            //  Stop if gesture need stop run
            if (!bRunning) break

//            Log.d("SensorInput", rawLine)
            //  Check query number for skip old events output

            if (!bQueryFound){
                bQueryFound = rawLine == "query$queryIx"
                continue
            }

 /*
            if (rawLine == "CLOSE_EVENTS")
                break
*/
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
            val timeLine = ev.groupValues[1].toDoubleOrNull()?:continue
            var timeout = timeLine - lastEventTime
//            Log.d("SensorInput", "event timeout:$timeout, old value:$lastEventTime, new value:$timeLine")
            if (timeout < -1.0) timeout = 1.0

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
            Log.d("SensorInput", "event device:$device")
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
        gesture.su.exec("kill -s SIGINT \$(jobs -p)")
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
