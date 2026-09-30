package com.ayc.coloraodtrace.hook

internal object TraceLifecycleHooks {
    private const val DOZE_SERVICE = "com.android.systemui.doze.DozeService"

    fun install(runtime: HookRuntime) {
        val clazz = runCatching { runtime.findClass(DOZE_SERVICE) }.getOrElse {
            TraceLog.warn("TRACE_REGISTER", "DozeService unavailable", it)
            return
        }

        clazz.declaredMethods
            .filter { it.name == "onDreamingStarted" && it.parameterCount == 0 }
            .forEachIndexed { index, method ->
                runCatching {
                    method.isAccessible = true
                    runtime.intercept("aod.trace.session-start.$index", method) { chain ->
                        val config = TraceConfigReader.read()
                        if (!config.enabled) return@intercept chain.proceed()

                        val session = TraceSession.begin()
                        TraceLog.info(
                            "AOD_SESSION_START",
                            "session=$session preset=${config.preset} stacks=${config.includeStacks} diffs=${config.includeFieldDiffs}",
                        )

                        val before = if (config.includeFieldDiffs) TraceSnapshot.capture(chain.getThisObject()) else emptyMap()
                        val stack = if (config.includeStacks) TraceSnapshot.stack(config.maxStackFrames) else ""
                        TraceLog.event(
                            "ENTER",
                            "DozeService",
                            TraceSnapshot.signature(method),
                            buildString {
                                if (before.isNotEmpty()) append("before={").append(TraceSnapshot.map(before)).append('}')
                                if (stack.isNotBlank()) append("|stack=").append(stack)
                            },
                        )
                        try {
                            val result = chain.proceed()
                            val after = if (config.includeFieldDiffs) TraceSnapshot.capture(chain.getThisObject()) else emptyMap()
                            TraceLog.event(
                                "EXIT",
                                "DozeService",
                                TraceSnapshot.signature(method),
                                "result=${TraceSnapshot.result(result)}|after={${TraceSnapshot.map(after)}}",
                            )
                            result
                        } catch (t: Throwable) {
                            TraceLog.event("THROW", "DozeService", method.name, "error=${t.javaClass.name}:${t.message}")
                            throw t
                        }
                    }
                }.onFailure {
                    TraceLog.warn("TRACE_REGISTER", "DozeService.onDreamingStarted failed", it)
                }
            }

        clazz.declaredMethods
            .filter { it.name == "onDreamingStopped" && it.parameterCount == 0 }
            .forEachIndexed { index, method ->
                runCatching {
                    method.isAccessible = true
                    runtime.intercept("aod.trace.session-stop.$index", method) { chain ->
                        val config = TraceConfigReader.read()
                        if (!config.enabled) return@intercept chain.proceed()

                        val elapsed = TraceSession.elapsedMs()
                        val stack = if (config.includeStacks) TraceSnapshot.stack(config.maxStackFrames) else ""
                        TraceLog.event(
                            "ENTER",
                            "DozeService",
                            TraceSnapshot.signature(method),
                            if (stack.isBlank()) "" else "stack=$stack",
                        )
                        try {
                            val result = chain.proceed()
                            TraceLog.event(
                                "EXIT",
                                "DozeService",
                                TraceSnapshot.signature(method),
                                "result=${TraceSnapshot.result(result)}",
                            )
                            val session = TraceSession.sessionId()
                            TraceLog.info("AOD_SESSION_END", "session=$session elapsedMs=$elapsed")
                            TraceSession.end()
                            result
                        } catch (t: Throwable) {
                            TraceLog.event("THROW", "DozeService", method.name, "error=${t.javaClass.name}:${t.message}")
                            throw t
                        }
                    }
                }.onFailure {
                    TraceLog.warn("TRACE_REGISTER", "DozeService.onDreamingStopped failed", it)
                }
            }
    }
}
