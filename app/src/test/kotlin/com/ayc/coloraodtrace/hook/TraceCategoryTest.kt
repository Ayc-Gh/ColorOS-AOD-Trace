package com.ayc.coloraodtrace.hook

import com.ayc.coloraodtrace.data.TraceConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceCategoryTest {
    @Test
    fun basicIsAlwaysEnabled() {
        (TraceConfig.PRESET_BASIC..TraceConfig.PRESET_WAKE).forEach {
            assertTrue(TraceCategory.BASIC.enabledFor(it))
        }
    }

    @Test
    fun panoramicPresetIncludesDisplayAndRules() {
        assertTrue(TraceCategory.PANORAMIC.enabledFor(TraceConfig.PRESET_PANORAMIC))
        assertTrue(TraceCategory.DISPLAY.enabledFor(TraceConfig.PRESET_PANORAMIC))
        assertTrue(TraceCategory.RULES.enabledFor(TraceConfig.PRESET_PANORAMIC))
        assertFalse(TraceCategory.WAKE.enabledFor(TraceConfig.PRESET_PANORAMIC))
    }

    @Test
    fun fullPresetIncludesEverything() {
        TraceCategory.entries.forEach {
            assertTrue(it.enabledFor(TraceConfig.PRESET_FULL))
        }
    }

    @Test
    fun wakePresetDoesNotEnableDisplayOrPanoramic() {
        assertTrue(TraceCategory.WAKE.enabledFor(TraceConfig.PRESET_WAKE))
        assertFalse(TraceCategory.DISPLAY.enabledFor(TraceConfig.PRESET_WAKE))
        assertFalse(TraceCategory.PANORAMIC.enabledFor(TraceConfig.PRESET_WAKE))
    }
}
