package com.ayc.coloraod.trace.config

data class TraceConfig(
    val enabled: Boolean = true,
    val profile: TraceProfile = TraceProfile.FULL,
    val captureStacks: Boolean = true,
    val captureFieldDiffs: Boolean = true,
    val maxStackDepth: Int = 12,
)
