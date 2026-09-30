package com.ayc.coloraod.trace.hook

import android.util.Log

internal object TraceLog {
    const val TAG = "AOD_Trace"
    private const val MAX_MESSAGE_CHARS = 7600

    fun d(event: String, message: String) = write(Log.DEBUG, event, message, null)
    fun i(event: String, message: String) = write(Log.INFO, event, message, null)
    fun w(event: String, message: String, error: Throwable? = null) = write(Log.WARN, event, message, error)
    fun e(event: String, message: String, error: Throwable? = null) = write(Log.ERROR, event, message, error)

    private fun write(priority: Int, event: String, message: String, error: Throwable?) {
        val body = buildString {
            append(message.take(MAX_MESSAGE_CHARS))
            if (error != null) {
                append(" error=").append(error.javaClass.name)
                error.message?.let { append(":").append(it.take(1000)) }
                append(" stack=").append(error.stackTraceToString().replace('\n', ' ').take(MAX_MESSAGE_CHARS))
            }
        }
        Log.println(priority, TAG, "$event: $body")
    }
}
