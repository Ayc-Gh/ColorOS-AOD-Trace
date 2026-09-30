package com.ayc.coloraod.trace.hook

import android.content.SharedPreferences
import com.ayc.coloraod.trace.config.TraceConfig
import com.ayc.coloraod.trace.config.TraceConfigStore
import com.ayc.coloraod.trace.config.TraceProfile
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal object TraceConfigReader {
    private val prefsRef = AtomicReference<SharedPreferences?>(null)
    private val cachedRef = AtomicReference(TraceConfig())
    private val lastRefreshNs = AtomicLong(0L)

    fun bindPrefs(prefs: SharedPreferences) {
        prefsRef.set(prefs)
        lastRefreshNs.set(0L)
        refresh(force = true)
    }

    fun read(): TraceConfig {
        refresh(force = false)
        return cachedRef.get()
    }

    private fun refresh(force: Boolean) {
        val now = System.nanoTime()
        if (!force && now - lastRefreshNs.get() < CACHE_TTL_NS) return
        val prefs = prefsRef.get() ?: return
        val all = runCatching { prefs.all }.getOrElse {
            TraceLog.e("CONFIG_READ", "remote preferences read failed", it)
            return
        }
        val config = TraceConfig(
            enabled = all[TraceConfigStore.KEY_ENABLED] as? Boolean ?: true,
            profile = TraceProfile.fromWireName(all[TraceConfigStore.KEY_PROFILE] as? String),
            captureStacks = all[TraceConfigStore.KEY_CAPTURE_STACKS] as? Boolean ?: true,
            captureFieldDiffs = all[TraceConfigStore.KEY_CAPTURE_FIELD_DIFFS] as? Boolean ?: true,
            maxStackDepth = ((all[TraceConfigStore.KEY_MAX_STACK_DEPTH] as? Number)?.toInt() ?: 12)
                .coerceIn(4, 32),
        )
        cachedRef.set(config)
        lastRefreshNs.set(now)
    }

    private const val CACHE_TTL_NS = 500_000_000L
}
