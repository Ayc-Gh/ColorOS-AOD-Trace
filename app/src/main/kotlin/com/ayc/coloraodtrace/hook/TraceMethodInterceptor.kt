package com.ayc.coloraodtrace.hook

import java.lang.reflect.Method

internal object TraceMethodInterceptor {
    fun install(
        runtime: HookRuntime,
        className: String,
        source: String,
        category: TraceCategory,
        exact: Set<String> = emptySet(),
        prefixes: List<String> = emptyList(),
        contains: List<String> = emptyList(),
        forceStack: Boolean = false,
    ) {
        val methods = runCatching { runtime.declaredMethods(className) }.getOrElse {
            TraceLog.warn("TRACE_REGISTER", "class=$className status=unavailable", it)
            return
        }.filter { method ->
            method.name in exact ||
                prefixes.any { method.name.startsWith(it, ignoreCase = true) } ||
                contains.any { method.name.contains(it, ignoreCase = true) }
        }.distinctBy { "${it.name}:${it.parameterTypes.joinToString(",") { p -> p.name }}" }

        if (methods.isEmpty()) {
            TraceLog.warn("TRACE_REGISTER", "class=$className source=$source status=no-matched-methods")
            return
        }

        methods.forEachIndexed { index, method ->
            runCatching {
                runtime.intercept(
                    id = "aod.trace.${source}.${method.name}.$index",
                    method = method,
                ) { chain ->
                    val config = TraceConfigReader.read()
                    if (!config.enabled || !category.enabledFor(config.preset)) {
                        return@intercept chain.proceed()
                    }

                    val target = chain.getThisObject()
                    val before = if (config.includeFieldDiffs) TraceSnapshot.capture(target) else emptyMap()
                    val args = TraceSnapshot.args(method) { argIndex ->
                        runCatching { chain.getArg(argIndex) }.getOrNull()
                    }
                    val stack = if (config.includeStacks) {
                        TraceSnapshot.stack(config.maxStackFrames)
                    } else {
                        ""
                    }

                    TraceLog.event(
                        phase = "ENTER",
                        source = source,
                        method = TraceSnapshot.signature(method),
                        detail = buildString {
                            append("args=[").append(args).append(']')
                            if (before.isNotEmpty()) append("|before={").append(TraceSnapshot.map(before)).append('}')
                            if (stack.isNotBlank()) append("|stack=").append(stack)
                        },
                    )

                    try {
                        val result = chain.proceed()
                        val after = if (config.includeFieldDiffs) TraceSnapshot.capture(target) else emptyMap()
                        val diff = if (config.includeFieldDiffs) TraceSnapshot.diff(before, after) else ""

                        TraceLog.event(
                            phase = "EXIT",
                            source = source,
                            method = TraceSnapshot.signature(method),
                            detail = buildString {
                                append("result=").append(TraceSnapshot.result(result))
                                if (after.isNotEmpty()) append("|after={").append(TraceSnapshot.map(after)).append('}')
                                if (diff.isNotBlank()) append("|diff={").append(diff).append('}')
                            },
                        )
                        result
                    } catch (t: Throwable) {
                        TraceLog.event(
                            phase = "THROW",
                            source = source,
                            method = TraceSnapshot.signature(method),
                            detail = "error=${t.javaClass.name}:${t.message}",
                        )
                        throw t
                    }
                }
            }.onSuccess {
                TraceLog.info(
                    "TRACE_REGISTER",
                    "source=$source method=${TraceSnapshot.signature(method)} category=$category status=registered",
                )
            }.onFailure {
                TraceLog.warn(
                    "TRACE_REGISTER",
                    "source=$source method=${TraceSnapshot.signature(method)} status=failed",
                    it,
                )
            }
        }
    }

    fun installExactMethod(
        runtime: HookRuntime,
        method: Method,
        source: String,
        category: TraceCategory,
        forceStack: Boolean = false,
    ) {
        runCatching {
            method.isAccessible = true
            runtime.intercept("aod.trace.$source.${method.name}", method) { chain ->
                val config = TraceConfigReader.read()
                if (!config.enabled || !category.enabledFor(config.preset)) {
                    return@intercept chain.proceed()
                }
                val target = chain.getThisObject()
                val before = if (config.includeFieldDiffs) TraceSnapshot.capture(target) else emptyMap()
                val args = TraceSnapshot.args(method) { i -> runCatching { chain.getArg(i) }.getOrNull() }
                val stack = if (config.includeStacks) TraceSnapshot.stack(config.maxStackFrames) else ""

                TraceLog.event(
                    "ENTER",
                    source,
                    TraceSnapshot.signature(method),
                    buildString {
                        append("args=[").append(args).append(']')
                        if (before.isNotEmpty()) append("|before={").append(TraceSnapshot.map(before)).append('}')
                        if (stack.isNotBlank()) append("|stack=").append(stack)
                    },
                )

                try {
                    val result = chain.proceed()
                    val after = if (config.includeFieldDiffs) TraceSnapshot.capture(target) else emptyMap()
                    val diff = if (config.includeFieldDiffs) TraceSnapshot.diff(before, after) else ""
                    TraceLog.event(
                        "EXIT",
                        source,
                        TraceSnapshot.signature(method),
                        buildString {
                            append("result=").append(TraceSnapshot.result(result))
                            if (after.isNotEmpty()) append("|after={").append(TraceSnapshot.map(after)).append('}')
                            if (diff.isNotBlank()) append("|diff={").append(diff).append('}')
                        },
                    )
                    result
                } catch (t: Throwable) {
                    TraceLog.event(
                        "THROW",
                        source,
                        TraceSnapshot.signature(method),
                        "error=${t.javaClass.name}:${t.message}",
                    )
                    throw t
                }
            }
        }.onFailure {
            TraceLog.warn("TRACE_REGISTER", "source=$source method=${method.name} status=failed", it)
        }
    }
}
