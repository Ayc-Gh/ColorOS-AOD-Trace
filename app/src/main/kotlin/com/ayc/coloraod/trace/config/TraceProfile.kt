package com.ayc.coloraod.trace.config

enum class TraceProfile(val wireName: String, val title: String) {
    BASIC("basic", "基础"),
    FULL("full", "全量"),
    PANORAMIC("panoramic", "全景 AOD"),
    DISPLAY("display", "Display"),
    WAKE("wake", "唤醒/退出");

    companion object {
        fun fromWireName(value: String?): TraceProfile =
            entries.firstOrNull { it.wireName == value } ?: FULL
    }
}
