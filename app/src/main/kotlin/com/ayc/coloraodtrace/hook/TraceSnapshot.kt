package com.ayc.coloraodtrace.hook

import android.view.Display
import android.view.View
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object TraceSnapshot {
    private val candidateFields = arrayOf(
        "mReason",
        "mRequestState",
        "mRequestedDisplayState",
        "mDeviceDisplayState",
        "mPendingScreenState",
        "mState",
        "mWakefulness",
        "mPerformAodType",
        "mAODProcessType",
        "mKgShowingWhileGoingToSleep",
        "currentUiState",
        "gotoDozeWithOff",
        "isSupportSmoothTransition",
        "mIsHideBySpecialRule",
        "mAodIsInShow",
        "mIsShowing",
        "isShowing",
        "mShow",
        "mVisible",
        "mIsVisible",
        "mDozing",
        "mDreaming",
    )

    private val displayStateFields = setOf(
        "mRequestState",
        "mRequestedDisplayState",
        "mDeviceDisplayState",
        "mPendingScreenState",
    )

    fun capture(target: Any?): Map<String, String> {
        if (target == null) return mapOf("target" to "null")
        val out = linkedMapOf<String, String>()
        out["class"] = target.javaClass.name

        if (target is View) {
            out["view.alpha"] = target.alpha.toString()
            out["view.visibility"] = visibilityName(target.visibility)
            out["view.isShown"] = target.isShown.toString()
        }

        candidateFields.forEach { name ->
            val value = readField(target, name) ?: return@forEach
            out[name] = if (name in displayStateFields && value is Number) {
                displayState(value.toInt())
            } else {
                safeValue(value)
            }
        }

        runCatching {
            target.javaClass.methods
                .firstOrNull { it.name == "isDreaming" && it.parameterCount == 0 }
                ?.let { out["isDreaming()"] = safeValue(it.invoke(target)) }
        }
        return out
    }

    fun diff(before: Map<String, String>, after: Map<String, String>): String =
        (before.keys + after.keys)
            .distinct()
            .mapNotNull { key ->
                val a = before[key]
                val b = after[key]
                if (a == b) null else "$key:${a ?: "<missing>"}->${b ?: "<missing>"}"
            }
            .joinToString(",")

    fun args(method: Method, getter: (Int) -> Any?): String =
        (0 until method.parameterCount).joinToString(",") { index ->
            "a$index=${formatArg(method, index, getter(index))}"
        }

    fun result(value: Any?): String = safeValue(value)

    fun map(values: Map<String, String>): String = values.entries
        .joinToString(",") { "${it.key}=${it.value}" }

    fun stack(maxFrames: Int): String = Thread.currentThread().stackTrace
        .asSequence()
        .filterNot {
            it.className.startsWith("java.lang.Thread") ||
                it.className.startsWith("java.lang.reflect") ||
                it.className.startsWith("java.lang.invoke") ||
                it.className.startsWith("io.github.libxposed") ||
                it.className.startsWith("com.ayc.coloraodtrace") ||
                it.className.contains("LSPHooker")
        }
        .take(maxFrames)
        .joinToString("<-") { "${it.className}#${it.methodName}:${it.lineNumber}" }

    fun signature(method: Method): String =
        "${method.name}(${method.parameterTypes.joinToString(",") { it.simpleName }}):${method.returnType.simpleName}"

    private fun formatArg(method: Method, index: Int, value: Any?): String {
        if (value is Int) {
            val stateLike =
                method.name.contains("ScreenState", ignoreCase = true) ||
                    method.name == "requestScreenState" ||
                    method.name == "setDozeScreenState" ||
                    method.name == "onScreenStateChanged"
            if (stateLike) return displayState(value)
        }
        return safeValue(value)
    }

    private fun readField(target: Any, name: String): Any? {
        var type: Class<*>? = target.javaClass
        while (type != null && type != Any::class.java) {
            val current = type
            val field: Field? = runCatching { current.getDeclaredField(name) }.getOrNull()
            if (field != null) {
                runCatching { field.isAccessible = true }
                return runCatching { field.get(target) }.getOrNull()
            }
            type = current.superclass
        }
        return null
    }

    private fun safeValue(value: Any?): String = when (value) {
        null -> "null"
        is CharSequence -> "\"${value.toString().take(160).replace("|", "/")}\""
        is Number, is Boolean -> value.toString()
        is Enum<*> -> "${value.javaClass.simpleName}.${value.name}"
        else -> "${value.javaClass.simpleName}@${Integer.toHexString(System.identityHashCode(value))}"
    }

    private fun displayState(value: Int): String = when (value) {
        Display.STATE_UNKNOWN -> "UNKNOWN(0)"
        Display.STATE_OFF -> "OFF(1)"
        Display.STATE_ON -> "ON(2)"
        Display.STATE_DOZE -> "DOZE(3)"
        Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND(4)"
        else -> value.toString()
    }

    private fun visibilityName(value: Int): String = when (value) {
        View.VISIBLE -> "VISIBLE"
        View.INVISIBLE -> "INVISIBLE"
        View.GONE -> "GONE"
        else -> value.toString()
    }
}
