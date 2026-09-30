package com.ayc.coloraodtrace.hook

import android.util.Log

internal object TraceLog {
    private const val TAG = "AOD_Trace"
    private const val MAX_CHARS = 3900

    fun event(
        phase: String,
        source: String,
        method: String,
        detail: String = "",
    ) {
        val message = buildString {
            append("AODT")
            append("|event=").append(TraceSession.nextEventId())
            append("|session=").append(TraceSession.sessionId())
            append("|elapsedMs=").append(TraceSession.elapsedMs())
            append("|thread=").append(Thread.currentThread().name.replace('|', '/'))
            append("|phase=").append(phase)
            append("|source=").append(source)
            append("|method=").append(method)
            if (detail.isNotBlank()) append('|').append(detail)
        }.replace('\n', ' ').take(MAX_CHARS)
        Log.i(TAG, message)
    }

    fun info(event: String, detail: String) {
        Log.i(TAG, "$event: ${detail.replace('\n', ' ').take(MAX_CHARS)}")
    }

    fun warn(event: String, detail: String, error: Throwable? = null) {
        val suffix = error?.let { " error=${it.javaClass.name}:${it.message}" } ?: ""
        Log.w(TAG, "$event: ${detail.replace('\n', ' ').take(MAX_CHARS)}$suffix")
    }
}
