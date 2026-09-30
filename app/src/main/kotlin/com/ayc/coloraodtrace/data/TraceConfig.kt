package com.ayc.coloraodtrace.data

data class TraceConfig(
    val enabled: Boolean = true,
    val preset: Int = PRESET_FULL,
    val includeStacks: Boolean = true,
    val includeFieldDiffs: Boolean = true,
    val maxStackFrames: Int = 12,
) {
    companion object {
        const val PRESET_BASIC = 0
        const val PRESET_FULL = 1
        const val PRESET_PANORAMIC = 2
        const val PRESET_DISPLAY = 3
        const val PRESET_WAKE = 4

        const val MIN_STACK_FRAMES = 4
        const val MAX_STACK_FRAMES = 24

        fun presetName(value: Int): String = when (value) {
            PRESET_BASIC -> "基础"
            PRESET_FULL -> "完整"
            PRESET_PANORAMIC -> "Panoramic 深度"
            PRESET_DISPLAY -> "Display/Doze 深度"
            PRESET_WAKE -> "Wake/Unlock 深度"
            else -> "完整"
        }
    }
}
