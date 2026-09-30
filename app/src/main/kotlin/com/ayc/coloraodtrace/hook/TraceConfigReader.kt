package com.ayc.coloraodtrace.hook

import android.content.SharedPreferences
import com.ayc.coloraodtrace.data.TraceConfig
import com.ayc.coloraodtrace.data.TraceConfigStore
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal object TraceConfigReader {
    private val prefsRef = AtomicReference<SharedPreferences?>(null)
    private val cachedRef = AtomicReference(TraceConfig())
    private val lastRefreshNs = AtomicLong(0L)

    fun bindPrefs(prefs: SharedPreferences) {
        prefsRef.set(prefs)
        lastRefreshNs.set(0L)
    }

    fun read(): TraceConfig {
        val now = System.nanoTime()
        if (now - lastRefreshNs.get() >= 500_000_000L) {
            prefsRef.get()?.let { prefs ->
                runCatching {
                    TraceConfig(
                        enabled = prefs.getBoolean(TraceConfigStore.KEY_ENABLED, true),
                        preset = prefs.getInt(TraceConfigStore.KEY_PRESET, TraceConfig.PRESET_FULL)
                            .coerceIn(TraceConfig.PRESET_BASIC, TraceConfig.PRESET_WAKE),
                        includeStacks = prefs.getBoolean(TraceConfigStore.KEY_INCLUDE_STACKS, true),
                        includeFieldDiffs = prefs.getBoolean(TraceConfigStore.KEY_INCLUDE_FIELD_DIFFS, true),
                        maxStackFrames = prefs.getInt(TraceConfigStore.KEY_MAX_STACK_FRAMES, 12)
                            .coerceIn(TraceConfig.MIN_STACK_FRAMES, TraceConfig.MAX_STACK_FRAMES),
                    )
                }.onSuccess { cachedRef.set(it) }
                lastRefreshNs.set(now)
            }
        }
        return cachedRef.get()
    }
}
