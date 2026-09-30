package com.ayc.coloraod.trace.config

import org.junit.Assert.assertEquals
import org.junit.Test

class TraceProfileTest {
    @Test
    fun unknownProfileFallsBackToFull() {
        assertEquals(TraceProfile.FULL, TraceProfile.fromWireName("future-profile"))
    }

    @Test
    fun knownProfileRoundTrips() {
        for (profile in TraceProfile.entries) {
            assertEquals(profile, TraceProfile.fromWireName(profile.wireName))
        }
    }
}
