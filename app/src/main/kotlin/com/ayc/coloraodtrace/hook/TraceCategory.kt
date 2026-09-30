package com.ayc.coloraodtrace.hook

import com.ayc.coloraodtrace.data.TraceConfig

internal enum class TraceCategory {
    BASIC,
    DISPLAY,
    PANORAMIC,
    WAKE,
    RULES;

    fun enabledFor(preset: Int): Boolean = when (this) {
        BASIC -> true
        DISPLAY -> preset == TraceConfig.PRESET_FULL ||
            preset == TraceConfig.PRESET_DISPLAY ||
            preset == TraceConfig.PRESET_PANORAMIC
        PANORAMIC -> preset == TraceConfig.PRESET_FULL ||
            preset == TraceConfig.PRESET_PANORAMIC
        WAKE -> preset == TraceConfig.PRESET_FULL ||
            preset == TraceConfig.PRESET_WAKE
        RULES -> preset == TraceConfig.PRESET_FULL ||
            preset == TraceConfig.PRESET_PANORAMIC
    }
}
