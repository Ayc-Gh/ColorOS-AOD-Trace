package com.ayc.coloraodtrace.hook

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

internal object TraceSession {
    private val sessionSeq = AtomicLong(0L)
    private val eventSeq = AtomicLong(0L)

    @Volatile private var activeSessionId = 0L
    @Volatile private var sessionStartElapsed = 0L

    fun begin(): Long {
        val id = sessionSeq.incrementAndGet()
        activeSessionId = id
        sessionStartElapsed = SystemClock.elapsedRealtime()
        eventSeq.set(0L)
        return id
    }

    fun end(): Long {
        val id = activeSessionId
        activeSessionId = 0L
        sessionStartElapsed = 0L
        return id
    }

    fun sessionId(): Long = activeSessionId

    fun elapsedMs(): Long {
        val start = sessionStartElapsed
        return if (start <= 0L) -1L else (SystemClock.elapsedRealtime() - start).coerceAtLeast(0L)
    }

    fun nextEventId(): Long = eventSeq.incrementAndGet()
}
