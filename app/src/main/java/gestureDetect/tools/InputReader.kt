package gestureDetect.tools

import SuperSU.ShellSU
import android.content.Context
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlin.concurrent.thread

class InputReader(val context: Context,
                  val su: ShellSU = ShellSU())
    : Observable<InputReader.EvData>()
{
    data class EvData(
            val device      : String,
            val evName      : String,
            val evButton    : String,
            val evPress     : String,
            val evTimeout   : Int
    )

    private var bPause = false

    private var inputDevices  = emptyList<String>()
    private var threadHandler : Thread? = null
    private var rxEmitter = PublishSubject.create<EvData>()
    private val composites = CompositeDisposable()
    private var cmdName : String? = null

    fun setDevices(devices : List<String>)
    {
        if (inputDevices == devices && threadHandler != null)
            return

        inputDevices = devices
        composites.clear()
        if (devices.isEmpty()) return

        if (!rxEmitter.hasObservers()) return
        rxEmitter.doOnDispose {
            synchronized(inputDevices) {
                 threadHandler = null
            }
            //  todo: sent any messages to flush buffer and self kill threads
        }

        composites += su.su.rxRootEnable
                .filter { it }
                .subscribe {
                    synchronized(inputDevices) {
                        threadHandler = getThread()
                    }
                }

        if (!su.checkRootAccess()) return
    }

    fun pause(bSetPause : Boolean){
        bPause = bSetPause
    }

    override fun subscribeActual(observer: Observer<in EvData>?) {
        observer?.also {
            rxEmitter.subscribe(it)
            setDevices(inputDevices)
        }
    }

    private fun hasObservers()
        = rxEmitter.hasObservers() && isThread()

    private fun isThread()
        = synchronized(inputDevices){
            threadHandler?.id == Thread.currentThread().id
        }

    private fun getThread() = thread {

        if (!threadLoopNew()) threadLoop()
        synchronized(inputDevices) {
            if (isThread()) threadHandler = null
        }
    }

    private fun threadLoopNew(): Boolean
    {
        if (cmdName == null)
        {
            val reader = UnpackEventReader(context)
            cmdName = reader.copyResourceTo(
                    "EventReader.so",
                    "EventReader")
                    ?: return false
            su.exec("chmod 777 $cmdName")
        }

        val arg = inputDevices.joinToString(" ")
        su.exec("$cmdName $arg&")
//        Runtime.getRuntime().exec("$cmdName $arg&")

        val regSplit = Regex("\\[\\s*([^\\s]+)\\]\\s*([^\\s]+):\\s+([^\\s]+)\\s+([^\\s]+)\\s*([^\\s]+)")

        while(hasObservers()) {
            //  Read line from input
            val rawLine = su.readErrorLine() ?: break
            //  Stop if gesture need stop run
            if (!hasObservers()) break
            //  Check device is near screen
            if (bPause) continue

            val ev = regSplit.find(rawLine) ?: continue
            val timeout = ev.groupValues[1].toDoubleOrNull()?:continue

            if (ev.groupValues[3] != "EV_KEY") continue
            val it = EvData(
                    ev.groupValues[2],
                    ev.groupValues[3],
                    ev.groupValues[4],
                    ev.groupValues[5],
                    (timeout*1000).toInt()
            )
            onNext(it)
        }

        su.killJobs()
        return true
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
        var lastEventTime = 0.0
        var lastKeyEventTime = 0.0
        var eqEvents = emptyList<String>()

        while(hasObservers())
        {
            //  Read line from input
            val rawLine = su.readErrorLine() ?: break
            //  Stop if gesture need stop run
            if (!hasObservers()) break

            //  Check query number for skip old events output

            if (!bQueryFound){
                bQueryFound = rawLine == "query$queryIx"
                continue
            }

            //  Check device is near screen
            if (bPause) continue
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

            if (ev.groupValues[2] != "EV_KEY") continue

            val it = EvData(
                    device,
                    ev.groupValues[2],
                    ev.groupValues[3],
                    ev.groupValues[4],
                    ((timeLine - lastKeyEventTime)*1000).toInt())

            lastKeyEventTime = timeLine

            onNext(it)
        }
        su.killJobs()
    }

    /**
     * Exec command for get events from getevent linux binary
     */
    private fun evCmd(queryIx:Long, ix:Int, device : String, nLimit : Int, nRepeat : Int)
    {
        if (nLimit > 0) {
            val CR = "\\\\n"
            val seq = (1..nRepeat).joinToString(" ")
            su.exec("while true ; do v$ix=\$(getevent -c $nLimit -tl $device | grep EV_KEY) ; [ \"\$v$ix\" ] && for i in $seq ; do echo query$queryIx$CR$device$CR\"\$v$ix\">&2 ; done ; done &")
        }else{
            su.exec("getevent -tl $device | grep EV_KEY>&2 &")
        }
    }
    private fun onNext(event : EvData)
    {
        rxEmitter.onNext(event)
    }
}