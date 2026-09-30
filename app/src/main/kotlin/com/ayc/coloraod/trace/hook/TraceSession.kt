package com.ayc.coloraod.trace.hook

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

internal object TraceSession {
    private val sequence = AtomicLong(0L)

    @Volatile private var currentId = 0L
    @Volatile private var startedAtMs = 0L

    fun begin(): Long {
        val id = sequence.incrementAndGet()
        currentId = id
        startedAtMs = SystemClock.elapsedRealtime()
        return id
    }

    fun end(): Long {
        val id = currentId
        currentId = 0L
        startedAtMs = 0L
        return id
    }

    fun id(): Long = currentId

    fun elapsedMs(): Long {
        val start = startedAtMs
        return if (start == 0L) -1L else (SystemClock.elapsedRealtime() - start).coerceAtLeast(0L)
    }
}
