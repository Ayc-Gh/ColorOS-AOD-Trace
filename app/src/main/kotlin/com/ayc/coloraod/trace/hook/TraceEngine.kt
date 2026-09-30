package com.ayc.coloraod.trace.hook

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

internal object TraceEngine {
    fun trace(
        source: String,
        category: TraceCategory,
        method: Method,
        chain: XposedInterface.Chain,
        forceStack: Boolean = false,
    ): Any? {
        if (!TracePolicy.enabled(category)) return chain.proceed()

        val config = TraceConfigReader.read()
        val target = chain.getThisObject()
        val before = TraceSnapshot.snapshot(target)
        val session = TraceSession.id()
        val elapsed = TraceSession.elapsedMs()
        val args = TraceSnapshot.arguments(method, chain)
        val stack = if (config.captureStacks && forceStack) {
            " stack=${TraceSnapshot.callerStack(config.maxStackDepth)}"
        } else {
            ""
        }

        TraceLog.i(
            "TRACE_ENTER",
            "session=$session elapsedMs=$elapsed category=$category source=$source " +
                "method=${TraceSnapshot.signature(method)} args=[$args] " +
                "snapshot={${TraceSnapshot.formatSnapshot(before)}}$stack",
        )

        return try {
            val result = chain.proceed()
            val after = TraceSnapshot.snapshot(target)
            TraceLog.i(
                "TRACE_EXIT",
                "session=${TraceSession.id()} elapsedMs=${TraceSession.elapsedMs()} category=$category " +
                    "source=$source method=${TraceSnapshot.signature(method)} " +
                    "result=${TraceSnapshot.result(source, method, result)} " +
                    "snapshot={${TraceSnapshot.formatSnapshot(after)}}",
            )

            if (config.captureFieldDiffs) {
                val diff = TraceSnapshot.diff(before, after)
                if (diff.isNotBlank()) {
                    TraceLog.i(
                        "FIELD_DIFF",
                        "session=${TraceSession.id()} elapsedMs=${TraceSession.elapsedMs()} " +
                            "source=$source method=${TraceSnapshot.signature(method)} changes={$diff}",
                    )
                }
            }
            result
        } catch (t: Throwable) {
            TraceLog.e(
                "TRACE_THROW",
                "session=${TraceSession.id()} elapsedMs=${TraceSession.elapsedMs()} " +
                    "source=$source method=${TraceSnapshot.signature(method)}",
                t,
            )
            throw t
        }
    }
}
