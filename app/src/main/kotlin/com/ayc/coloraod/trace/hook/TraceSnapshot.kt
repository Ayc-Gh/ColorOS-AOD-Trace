package com.ayc.coloraod.trace.hook

import android.view.Display
import android.view.View
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object TraceSnapshot {
    private val displayStateFields = setOf(
        "mRequestState",
        "mRequestedDisplayState",
        "mDeviceDisplayState",
        "mPendingScreenState",
        "mScreenState",
    )

    private val fields = arrayOf(
        "mReason",
        "mRequestState",
        "mRequestedDisplayState",
        "mDeviceDisplayState",
        "mPendingScreenState",
        "mScreenState",
        "mPerformAodType",
        "mAODProcessType",
        "mKgShowingWhileGoingToSleep",
        "mState",
        "mWakefulness",
        "mIsHideBySpecialRule",
        "mAodIsInShow",
        "mIsShowing",
        "mShowing",
        "mHide",
        "currentUiState",
        "gotoDozeWithOff",
        "isSupportSmoothTransition",
    )

    fun snapshot(target: Any?): Map<String, String> {
        if (target == null) return mapOf("target" to "null")
        val values = linkedMapOf<String, String>()
        values["class"] = target.javaClass.name

        for (name in fields) {
            val value = readField(target, name) ?: continue
            values[name] = if (name in displayStateFields && value is Number) {
                displayState(value.toInt())
            } else {
                safeValue(value)
            }
        }

        if (target is View) {
            values["view.alpha"] = target.alpha.toString()
            values["view.visibility"] = visibilityName(target.visibility)
            values["view.isShown"] = target.isShown.toString()
            values["view.width"] = target.width.toString()
            values["view.height"] = target.height.toString()
        }

        runCatching {
            val method = target.javaClass.methods.firstOrNull {
                it.name == "isDreaming" && it.parameterCount == 0
            }
            if (method != null) values["isDreaming"] = safeValue(method.invoke(target))
        }

        return values
    }

    fun arguments(method: Method, chain: XposedInterface.Chain): String =
        (0 until method.parameterCount).joinToString(", ") { index ->
            val value = runCatching { chain.getArg(index) }.getOrNull()
            "a$index=${formatArgument(method, value)}"
        }

    fun result(source: String, method: Method, value: Any?): String {
        val stateLike = value is Number &&
            source == "AODVirtualDozeClient" &&
            method.name == "getVoteState"
        return if (stateLike) displayState((value as Number).toInt()) else safeValue(value)
    }

    fun diff(before: Map<String, String>, after: Map<String, String>): String {
        val keys = linkedSetOf<String>().apply {
            addAll(before.keys)
            addAll(after.keys)
        }
        return keys.mapNotNull { key ->
            val old = before[key]
            val new = after[key]
            if (old == new) null else "$key:$old->$new"
        }.joinToString(" ")
    }

    fun formatSnapshot(values: Map<String, String>): String =
        values.entries.joinToString(" ") { "${it.key}=${it.value}" }

    fun callerStack(maxDepth: Int): String =
        Thread.currentThread().stackTrace
            .asSequence()
            .filterNot {
                it.className.contains("TraceSnapshot") ||
                    it.className.contains("TraceEngine") ||
                    it.className.contains("TraceInstaller") ||
                    it.className.contains("HookRuntime") ||
                    it.className.startsWith("java.lang.Thread") ||
                    it.className.startsWith("java.lang.reflect") ||
                    it.className.startsWith("java.lang.invoke") ||
                    it.className.startsWith("io.github.libxposed") ||
                    it.className.contains("LSPHooker")
            }
            .take(maxDepth)
            .joinToString(" <- ") { "${it.className}#${it.methodName}:${it.lineNumber}" }

    fun signature(method: Method): String =
        "${method.name}(${method.parameterTypes.joinToString(",") { it.simpleName }}):${method.returnType.simpleName}"

    private fun formatArgument(method: Method, value: Any?): String {
        val stateLike = value is Number && (
            method.name.contains("ScreenState", ignoreCase = true) ||
                method.name == "setDozeScreenState" ||
                method.name == "onScreenStateChanged" ||
                method.name.startsWith("requestScreenState")
            )
        return if (stateLike) displayState((value as Number).toInt()) else safeValue(value)
    }

    private fun safeValue(value: Any?): String = when (value) {
        null -> "null"
        is CharSequence -> """ + value.toString().take(240).replace("\n", " ") + """
        is Enum<*> -> "${value.javaClass.simpleName}.${value.name}"
        is Number, is Boolean, is Char -> value.toString()
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
        View.VISIBLE -> "VISIBLE(0)"
        View.INVISIBLE -> "INVISIBLE(4)"
        View.GONE -> "GONE(8)"
        else -> value.toString()
    }

    private fun readField(instance: Any, name: String): Any? {
        var type: Class<*>? = instance.javaClass
        while (type != null) {
            val current = type
            val field: Field? = runCatching { current.getDeclaredField(name) }.getOrNull()
            if (field != null) {
                runCatching { field.isAccessible = true }
                return runCatching { field.get(instance) }.getOrNull()
            }
            type = current.superclass
        }
        return null
    }
}
