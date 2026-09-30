package com.ayc.coloraod.trace.hook

import android.service.dreams.DreamService
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

internal object TraceInstaller {
    private const val DOZE_SERVICE = "com.android.systemui.doze.DozeService"

    fun install(runtime: HookRuntime) {
        installDozeLifecycle(runtime)
        installDreamFinish(runtime)

        installClass(
            runtime,
            "com.android.systemui.doze.DozeMachine",
            "DozeMachine",
            TraceCategory.DOZE,
            exact = setOf("requestState", "transitionTo", "transitionPolicy", "resolveIntermediateState"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.android.systemui.doze.DozeScreenState",
            "DozeScreenState",
            TraceCategory.DOZE,
            exact = setOf("transitionTo", "applyScreenState", "updatePendingScreenState"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.display.AODDisplayUtil",
            "AODDisplayUtil",
            TraceCategory.DISPLAY,
            exact = setOf("updateDisplayState", "onScreenStateChanged", "setScreenState"),
            prefixes = listOf("requestScreenState"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.display.BaseDisplayUtil",
            "BaseDisplayUtil",
            TraceCategory.DISPLAY,
            exact = setOf("setScreenState", "updateDisplayState", "onScreenStateChanged"),
            prefixes = listOf("requestScreenState"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.display.AODDisplayUtil\$AODVirtualDozeClient",
            "AODVirtualDozeClient",
            TraceCategory.DISPLAY,
            exact = setOf("getVoteState", "setRequestState", "requestState", "updateState"),
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.OplusDozeServiceExImpl",
            "OplusDozeServiceExImpl",
            TraceCategory.DOZE,
            exact = setOf("setDozeScreenState", "onDreamingStarted", "onDreamingStopped", "finish"),
            prefixes = listOf("requestScreenState", "setScreenState", "setDoze", "stopDoze"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.controller.PanoramicAodController",
            "PanoramicAodController",
            TraceCategory.PANORAMIC,
            exact = setOf("startShow", "updatePanoramicAodHideStatus"),
            prefixes = listOf("startPanoramicAodShowAnim", "updatePanoramic", "hidePanoramic", "stopPanoramic"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.controller.PanoramicAodController\$startPanoramicAodShowAnim\$1\$1",
            "PanoramicAnimatorListener",
            TraceCategory.PANORAMIC,
            exact = setOf("onAnimationStart", "onAnimationEnd", "onAnimationCancel"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.aodclock.off.AodBlackLayout",
            "AodBlackLayout",
            TraceCategory.UI,
            exact = setOf("onInvalidated", "onDescendantInvalidated", "onAttachedToWindow", "onDetachedFromWindow"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.aodclock.off.AodUpdateManager",
            "AodUpdateManager",
            TraceCategory.RULE,
            exact = setOf("needDisplayAodInSpecialRule", "setIsHideBySpecialRule"),
            prefixes = listOf("hideAod", "updateAod", "setIsHide", "needDisplay"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.aodclock.off.AodUpdateManager\$2",
            "AodUpdateManagerSensor",
            TraceCategory.RULE,
            exact = setOf("hideAodByDarkLight"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.display.SmoothTransitionController",
            "SmoothTransitionController",
            TraceCategory.PANORAMIC,
            exact = setOf("updateCurrentUiState"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.aod.display.OplusWakeUpController\$AodSingleClickWakeUpCallback",
            "AodSingleClickWakeUpCallback",
            TraceCategory.WAKE,
            exact = setOf("onClick"),
            forceStack = true,
        )
        installClass(
            runtime,
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintIcon",
            "OnScreenFingerprintIcon",
            TraceCategory.WAKE,
            exact = setOf("onTouchEvent"),
            forceStack = true,
        )
    }

    private fun installDozeLifecycle(runtime: HookRuntime) {
        val clazz = runCatching { runtime.findClass(DOZE_SERVICE) }.getOrElse {
            TraceLog.e("REGISTER", "DozeService unavailable", it)
            return
        }

        installDeclared(
            runtime,
            clazz,
            "DozeService",
            TraceCategory.DOZE,
            setOf("setDozeScreenState", "setDozeScreenBrightness"),
            emptyList(),
            true,
        )

        hookExact(runtime, clazz, "onDreamingStarted", "DozeService.onDreamingStarted") { method, chain ->
            val session = TraceSession.begin()
            TraceLog.i("SESSION_START", "session=$session")
            TraceEngine.trace("DozeService", TraceCategory.SESSION, method, chain, forceStack = true)
        }

        hookExact(runtime, clazz, "onDreamingStopped", "DozeService.onDreamingStopped") { method, chain ->
            val result = TraceEngine.trace("DozeService", TraceCategory.SESSION, method, chain, forceStack = true)
            val id = TraceSession.id()
            val elapsed = TraceSession.elapsedMs()
            TraceLog.i("SESSION_END", "session=$id elapsedMs=$elapsed")
            TraceSession.end()
            result
        }

        hookExact(runtime, clazz, "onWakeUp", "DozeService.onWakeUp") { method, chain ->
            TraceEngine.trace("DozeService", TraceCategory.WAKE, method, chain, forceStack = true)
        }
    }

    private fun installDreamFinish(runtime: HookRuntime) {
        runCatching {
            DreamService::class.java.getDeclaredMethod("finish").apply { isAccessible = true }
        }.onSuccess { method ->
            runCatching {
                runtime.intercept("aod.trace.DreamService.finish", method) { chain ->
                    TraceEngine.trace("DreamService", TraceCategory.SESSION, method, chain, forceStack = true)
                }
            }.onSuccess {
                TraceLog.i("REGISTER", "DreamService#finish registered")
            }.onFailure {
                TraceLog.w("REGISTER", "DreamService#finish hook failed", it)
            }
        }.onFailure {
            TraceLog.w("REGISTER", "DreamService#finish unavailable", it)
        }
    }

    private fun installClass(
        runtime: HookRuntime,
        className: String,
        source: String,
        category: TraceCategory,
        exact: Set<String> = emptySet(),
        prefixes: List<String> = emptyList(),
        forceStack: Boolean = false,
    ) {
        val clazz = runCatching { runtime.findClass(className) }.getOrElse {
            TraceLog.w("REGISTER_MISSING", "class=$className")
            return
        }
        installDeclared(runtime, clazz, source, category, exact, prefixes, forceStack)
    }

    private fun installDeclared(
        runtime: HookRuntime,
        clazz: Class<*>,
        source: String,
        category: TraceCategory,
        exact: Set<String>,
        prefixes: List<String>,
        forceStack: Boolean,
    ) {
        val methods = runCatching { clazz.declaredMethods.toList() }.getOrDefault(emptyList())
            .filter { method ->
                method.name in exact || prefixes.any { prefix -> method.name.startsWith(prefix) }
            }
            .distinctBy { "${it.name}:${it.parameterTypes.joinToString(",") { p -> p.name }}" }

        if (methods.isEmpty()) {
            TraceLog.w("REGISTER_MISSING", "source=$source matchedMethods=0")
            return
        }

        methods.forEachIndexed { index, method ->
            runCatching { method.isAccessible = true }
            val id = "aod.trace.$source.${method.name}.$index"
            runCatching {
                runtime.intercept(id, method) { chain ->
                    TraceEngine.trace(source, category, method, chain, forceStack)
                }
            }.onSuccess {
                TraceLog.i("REGISTER", "source=$source method=${TraceSnapshot.signature(method)}")
            }.onFailure {
                TraceLog.w("REGISTER", "source=$source method=${TraceSnapshot.signature(method)} failed", it)
            }
        }
    }

    private fun hookExact(
        runtime: HookRuntime,
        clazz: Class<*>,
        methodName: String,
        idSuffix: String,
        body: (Method, XposedInterface.Chain) -> Any?,
    ) {
        val methods = runCatching { clazz.declaredMethods.toList() }.getOrDefault(emptyList())
            .filter { it.name == methodName }
        if (methods.isEmpty()) {
            TraceLog.w("REGISTER_MISSING", "source=$idSuffix")
            return
        }

        methods.forEachIndexed { index, method ->
            runCatching { method.isAccessible = true }
            runCatching {
                runtime.intercept("aod.trace.$idSuffix.$index", method) { chain -> body(method, chain) }
            }.onSuccess {
                TraceLog.i("REGISTER", "source=$idSuffix method=${TraceSnapshot.signature(method)}")
            }.onFailure {
                TraceLog.w("REGISTER", "source=$idSuffix failed", it)
            }
        }
    }
}
