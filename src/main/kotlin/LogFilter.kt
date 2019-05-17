package space.anity

import ch.qos.logback.classic.*
import ch.qos.logback.classic.spi.*
import ch.qos.logback.core.filter.*
import ch.qos.logback.core.spi.*

class LogFilter : Filter<ILoggingEvent>() {
    private val level = Level.ERROR

    override fun decide(event: ILoggingEvent): FilterReply {
        return if (event.level.isGreaterOrEqual(level)) {
            FilterReply.NEUTRAL
        } else {
            if (!silent || event.message.contains("Help")) {
                FilterReply.ACCEPT
            } else {
                FilterReply.DENY
            }
        }
    }
}
