package com.ayc.coloraod.trace.config

import android.content.Context
import android.content.SharedPreferences
import com.ayc.coloraod.trace.TraceApplication
import io.github.libxposed.service.XposedService
import java.util.concurrent.atomic.AtomicReference

object TraceConfigStore {
    const val PREFS_NAME = "aod_trace_config"
    const val KEY_ENABLED = "enabled"
    const val KEY_PROFILE = "profile"
    const val KEY_CAPTURE_STACKS = "capture_stacks"
    const val KEY_CAPTURE_FIELD_DIFFS = "capture_field_diffs"
    const val KEY_MAX_STACK_DEPTH = "max_stack_depth"

    private val cached = AtomicReference(TraceConfig())

    private fun local(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun remote(): SharedPreferences? =
        TraceApplication.service()?.getRemotePreferences(PREFS_NAME)

    fun onXposedServiceBound(context: Context, service: XposedService) {
        val local = local(context)
        val remote = service.getRemotePreferences(PREFS_NAME)
        if (remote.all.isEmpty() && local.all.isNotEmpty()) {
            copyAll(local, remote)
        }
    }

    fun read(context: Context): TraceConfig {
        val prefs = remote() ?: local(context)
        val fresh = runCatching { fromMap(prefs.all) }.getOrNull()
        if (fresh != null) cached.set(fresh)
        return fresh ?: cached.get()
    }

    fun write(context: Context, config: TraceConfig): Boolean {
        val safe = config.copy(maxStackDepth = config.maxStackDepth.coerceIn(4, 32))
        val localOk = writePrefs(local(context), safe)
        val remotePrefs = remote()
        val remoteOk = remotePrefs?.let { writePrefs(it, safe) } ?: true
        if (localOk && remoteOk) cached.set(safe)
        return localOk && remoteOk
    }

    private fun writePrefs(prefs: SharedPreferences, config: TraceConfig): Boolean =
        runCatching {
            prefs.edit()
                .putBoolean(KEY_ENABLED, config.enabled)
                .putString(KEY_PROFILE, config.profile.wireName)
                .putBoolean(KEY_CAPTURE_STACKS, config.captureStacks)
                .putBoolean(KEY_CAPTURE_FIELD_DIFFS, config.captureFieldDiffs)
                .putInt(KEY_MAX_STACK_DEPTH, config.maxStackDepth)
                .commit()
        }.getOrDefault(false)

    private fun fromMap(all: Map<String, *>): TraceConfig = TraceConfig(
        enabled = all[KEY_ENABLED] as? Boolean ?: true,
        profile = TraceProfile.fromWireName(all[KEY_PROFILE] as? String),
        captureStacks = all[KEY_CAPTURE_STACKS] as? Boolean ?: true,
        captureFieldDiffs = all[KEY_CAPTURE_FIELD_DIFFS] as? Boolean ?: true,
        maxStackDepth = ((all[KEY_MAX_STACK_DEPTH] as? Number)?.toInt() ?: 12).coerceIn(4, 32),
    )

    private fun copyAll(from: SharedPreferences, to: SharedPreferences) {
        val edit = to.edit()
        for ((key, value) in from.all) {
            when (value) {
                is Boolean -> edit.putBoolean(key, value)
                is Int -> edit.putInt(key, value)
                is Long -> edit.putLong(key, value)
                is Float -> edit.putFloat(key, value)
                is String -> edit.putString(key, value)
            }
        }
        edit.commit()
    }
}
