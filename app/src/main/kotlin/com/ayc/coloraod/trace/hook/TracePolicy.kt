package com.ayc.coloraod.trace.hook

import com.ayc.coloraod.trace.config.TraceProfile

internal object TracePolicy {
    fun enabled(category: TraceCategory): Boolean {
        val config = TraceConfigReader.read()
        if (!config.enabled) return false
        return when (config.profile) {
            TraceProfile.FULL -> true
            TraceProfile.BASIC -> category in setOf(
                TraceCategory.SESSION,
                TraceCategory.DISPLAY,
                TraceCategory.DOZE,
            )
            TraceProfile.PANORAMIC -> category in setOf(
                TraceCategory.SESSION,
                TraceCategory.PANORAMIC,
                TraceCategory.UI,
                TraceCategory.DISPLAY,
            )
            TraceProfile.DISPLAY -> category in setOf(
                TraceCategory.SESSION,
                TraceCategory.DISPLAY,
                TraceCategory.DOZE,
            )
            TraceProfile.WAKE -> category in setOf(
                TraceCategory.SESSION,
                TraceCategory.WAKE,
                TraceCategory.DOZE,
                TraceCategory.DISPLAY,
            )
        }
    }
}
