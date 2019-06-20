package space.anity

import ch.qos.logback.classic.*
import ch.qos.logback.classic.spi.*
import ch.qos.logback.core.filter.*
import ch.qos.logback.core.spi.*

class LogFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent): FilterReply {
        val isUseful = event.loggerName !in
                listOf(
                    "Exposed",
                    "io.javalin.Javalin",
                    "com.fizzed.rocker.runtime.RockerRuntime",
                    "org.eclipse.jetty.util.log"
                )

        return if (event.level == Level.INFO && isUseful) {
            FilterReply.ACCEPT
        } else {
            if ((!silent || event.message.contains("Help")) && event.level != Level.DEBUG && isUseful) {
                FilterReply.ACCEPT
            } else {
                FilterReply.DENY
            }
        }
    }
}
