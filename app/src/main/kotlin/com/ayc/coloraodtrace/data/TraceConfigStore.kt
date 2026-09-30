package com.ayc.coloraodtrace.data

import android.content.Context
import android.content.SharedPreferences
import com.ayc.coloraodtrace.TraceApplication
import io.github.libxposed.service.XposedService

object TraceConfigStore {
    const val PREFS_NAME = "trace_config"
    const val KEY_ENABLED = "enabled"
    const val KEY_PRESET = "preset"
    const val KEY_INCLUDE_STACKS = "include_stacks"
    const val KEY_INCLUDE_FIELD_DIFFS = "include_field_diffs"
    const val KEY_MAX_STACK_FRAMES = "max_stack_frames"

    private fun local(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun remote(): SharedPreferences? =
        TraceApplication.service()?.getRemotePreferences(PREFS_NAME)

    fun onXposedServiceBound(context: Context, service: XposedService) {
        val local = local(context)
        val remote = service.getRemotePreferences(PREFS_NAME)
        if (remote.all.isEmpty()) writePrefs(remote, readPrefs(local))
    }

    fun read(context: Context): TraceConfig =
        runCatching { readPrefs(remote() ?: local(context)) }.getOrDefault(TraceConfig())

    fun write(context: Context, config: TraceConfig): Boolean {
        val safe = config.copy(
            preset = config.preset.coerceIn(TraceConfig.PRESET_BASIC, TraceConfig.PRESET_WAKE),
            maxStackFrames = config.maxStackFrames.coerceIn(
                TraceConfig.MIN_STACK_FRAMES,
                TraceConfig.MAX_STACK_FRAMES,
            ),
        )
        val localOk = writePrefs(local(context), safe)
        val remoteOk = remote()?.let { writePrefs(it, safe) } ?: true
        return localOk && remoteOk
    }

    private fun readPrefs(prefs: SharedPreferences): TraceConfig = TraceConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, true),
        preset = prefs.getInt(KEY_PRESET, TraceConfig.PRESET_FULL)
            .coerceIn(TraceConfig.PRESET_BASIC, TraceConfig.PRESET_WAKE),
        includeStacks = prefs.getBoolean(KEY_INCLUDE_STACKS, true),
        includeFieldDiffs = prefs.getBoolean(KEY_INCLUDE_FIELD_DIFFS, true),
        maxStackFrames = prefs.getInt(KEY_MAX_STACK_FRAMES, 12)
            .coerceIn(TraceConfig.MIN_STACK_FRAMES, TraceConfig.MAX_STACK_FRAMES),
    )

    private fun writePrefs(prefs: SharedPreferences, config: TraceConfig): Boolean =
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putInt(KEY_PRESET, config.preset)
            .putBoolean(KEY_INCLUDE_STACKS, config.includeStacks)
            .putBoolean(KEY_INCLUDE_FIELD_DIFFS, config.includeFieldDiffs)
            .putInt(KEY_MAX_STACK_FRAMES, config.maxStackFrames)
            .commit()
}
