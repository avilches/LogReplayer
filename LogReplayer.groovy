@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6' )

/*

Requirements:

    Groovy installed: sudo apt-get install groovy

Usage:

    groovy LogReplayer.groovy <host> <logFile>

Example:

    groovy LogReplayer.groovy http://localhost:8080 /var/log/apache2/localhost.log

How it works:

    The script read an Apache log and execute only the GET lines. The script use the timestamps from the log,
    replicating the original behavior.

    The output will be something like this

[6] 00.147s 200  http://localhost:8080/web/book/9788490192054?val=orr
[7] 00.058s 500! http://localhost:8080//location/free?ip=2.2.2.2 Internal Server Error

    * The [6] means now there are 6 connections opened against the server (concurrent users)
    * The second number (00.147s) means the time needed by the script to process the request
    * The third number (200, 500!) is the http code. 500! means an exception happens when processing the request
    * The fourth part is the url
    * The fifth part is the exception message, if available

 */

import groovyx.net.http.*
import java.text.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

if (args.size() < 2) {
    println "Usage: groovy LogReplayer.groovy <host> <logFile>\nExample: groovy LogReplayer.groovy http://localhost:8080 /var/log/apache2/localhost.log"
    return
}

new Replayer(host:args[0]).replay(args[1])

class Replayer {
    def threadPoolSize = 50, maxPendingTaskCount = 100 // configurable
    def host, firstLog = 0, currentConnections, executor

    def replay(fileName) {
        lastSecondToRun = -1
        currentConnections = new AtomicInteger(1)
        urlListForTheNextSecond = []
        executor = new ScheduledThreadPoolExecutor(threadPoolSize)
        def file = new File(fileName)
        def start = currentTimeSeconds
        file.eachLine { String line ->
            def o = parseLine(line)
            if (o) {
                def elapsed = currentTimeSeconds - start  // Elapsed time since script began
                def secondToRun = (o.gap - elapsed) as int // When the next http request should executed (in seconds)
//                println "[+] Queing request $idx: taskCount=$executor.taskCount completedTaskCount=$executor.completedTaskCount poolSize=${executor.poolSize}\r"
                launchUrlAt(o.url, secondToRun)
            }
            // Don't add to the executor queue more than maxPendingTaskCount tasks, so wait until some of them finish...
            while ((executor.taskCount - executor.completedTaskCount) >= maxPendingTaskCount) Thread.sleep(1000)
        }
        finish()
    }

    def getCurrentTimeSeconds() { (System.currentTimeMillis()/1000) as int }

    def finish() {
        if (executor && !executor.shutdown) {
            // Wait for the pending tasks finish
            while ((executor.taskCount - executor.completedTaskCount) > 0) Thread.sleep(100)
            executor.shutdown()
        }
    }

    def urlListForTheNextSecond, lastSecondToRun = -1
    private void launchUrlAt(url, long secondToRun) {
        if (lastSecondToRun != secondToRun && urlListForTheNextSecond) {
            // Apache log date appears in seconds, so probably we will have a lot of request in the same second.
            // To avoid lunch all of them at once (it won't be realistic), just store them in the
            // urlListForTheNextSecond buffer. When a log with a different date appears, it will executed all of
            // them, but spread in time
            spreadRequests()
            urlListForTheNextSecond.clear()
        }
        urlListForTheNextSecond << url
        lastSecondToRun = secondToRun
    }

    private void spreadRequests() {
        // So, if we have 3 urls for the same second, it will execute
        // the first at 0s, the second at 0.333s and the third at 0.666s
        def lag = (1000 / urlListForTheNextSecond.size()) as int
        def accumulated = 0
        urlListForTheNextSecond.each { String url ->
            def nextRun = (lastSecondToRun * 1000) + accumulated
            executor.schedule({
                long start = System.currentTimeMillis()
                try {
                    currentConnections.andIncrement
//                        println "[+] ${new DecimalFormat("00.000").format(nextRun/1000)} ${host}${url}"
                    new groovyx.net.http.HTTPBuilder(host + url).get([contentType: ContentType.TEXT]) { resp, txt ->
                        println "[${currentConnections}] ${new DecimalFormat("00.000").format((System.currentTimeMillis() - start) / 1000)}s ${resp.statusLine.statusCode}  ${host}${url}"
                    }
                } catch (e) {
                    println "[${currentConnections}] ${new DecimalFormat("00.000").format((System.currentTimeMillis() - start) / 1000)}s 500! ${host}${url} ${e.message}"
                }
                currentConnections.andDecrement
            }, Math.max(nextRun, 1), TimeUnit.MILLISECONDS)
            accumulated += lag
        }
    }

    def parseLine(line) {
        if (!line) return
        def dateString = line.substring(line.indexOf("[")+1, line.indexOf("]"))
        def date = new Date().parse("dd/MMM/yyyy:hh:mm:ss Z", dateString)
        firstLog = firstLog?:date.time
        def getPos = line.indexOf('"GET ');
        if (getPos > -1) {
            def url = line.substring(getPos + 5, line.indexOf(" ", getPos + 5))
            // En gap van los segundos de diferencia entre la primera linea del log y la actual
            return [url:url, gap: (date.time-firstLog)/1000]
        }
        null
    }

}