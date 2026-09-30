package com.ayc.coloraodtrace.hook

import android.service.dreams.DreamService
import com.ayc.coloraodtrace.data.TraceConfigStore
import io.github.libxposed.api.XposedModule

internal object TraceInstaller {
    fun install(module: XposedModule, classLoader: ClassLoader) {
        val runtime = HookRuntime(module, classLoader)
        TraceConfigReader.bindPrefs(module.getRemotePreferences(TraceConfigStore.PREFS_NAME))

        TraceLog.info("TRACE_BOOT", "installing passive ColorOS AOD full-chain tracer")
        TraceLifecycleHooks.install(runtime)
        installBasic(runtime)
        installDisplay(runtime)
        installPanoramic(runtime)
        installRules(runtime)
        installWake(runtime)
        TraceLog.info("TRACE_BOOT", "registration complete")
    }

    private fun installBasic(runtime: HookRuntime) {
        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.android.systemui.doze.DozeService",
            source = "DozeService",
            category = TraceCategory.BASIC,
            exact = setOf(
                "onWakeUp",
                "setDozeScreenState",
                "setDozeScreenBrightness",
            ),
            forceStack = true,
        )

        runCatching {
            DreamService::class.java.getDeclaredMethod("finish")
        }.onSuccess { method ->
            TraceMethodInterceptor.installExactMethod(
                runtime = runtime,
                method = method,
                source = "DreamService",
                category = TraceCategory.BASIC,
                forceStack = true,
            )
        }.onFailure {
            TraceLog.warn("TRACE_REGISTER", "DreamService.finish unavailable", it)
        }

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.OplusDozeServiceExImpl",
            source = "OplusDozeServiceExImpl",
            category = TraceCategory.BASIC,
            exact = setOf(
                "onDreamingStarted",
                "onDreamingStopped",
                "setDozeScreenState",
                "finish",
            ),
            prefixes = listOf("requestScreenState", "setDoze", "stopDoze"),
            forceStack = true,
        )
    }

    private fun installDisplay(runtime: HookRuntime) {
        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.display.AODDisplayUtil",
            source = "AODDisplayUtil",
            category = TraceCategory.DISPLAY,
            exact = setOf(
                "updateDisplayState",
                "onScreenStateChanged",
                "setScreenState",
                "requestScreenState",
                "requestScreenStateWhileDreamingStart",
                "requestScreenStateWhileDreamingStop",
                "requestScreenStateWhileDreaming",
            ),
            prefixes = listOf("requestScreenState"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.display.BaseDisplayUtil",
            source = "BaseDisplayUtil",
            category = TraceCategory.DISPLAY,
            exact = setOf(
                "setScreenState",
                "requestScreenState",
                "updateDisplayState",
                "onScreenStateChanged",
            ),
            prefixes = listOf("requestScreenState"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.display.AODDisplayUtil\$AODVirtualDozeClient",
            source = "AODVirtualDozeClient",
            category = TraceCategory.DISPLAY,
            exact = setOf("getVoteState", "setRequestState", "requestState", "updateState"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.android.systemui.doze.DozeMachine",
            source = "DozeMachine",
            category = TraceCategory.DISPLAY,
            exact = setOf("requestState", "transitionTo", "transitionPolicy", "resolveIntermediateState"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.android.systemui.doze.DozeScreenState",
            source = "DozeScreenState",
            category = TraceCategory.DISPLAY,
            exact = setOf("transitionTo", "applyScreenState", "updatePendingScreenState"),
            forceStack = true,
        )
    }

    private fun installPanoramic(runtime: HookRuntime) {
        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.controller.PanoramicAodController",
            source = "PanoramicAodController",
            category = TraceCategory.PANORAMIC,
            exact = setOf(
                "startShow",
                "updatePanoramicAodHideStatus",
                "startPanoramicAodShowAnim",
                "startPanoramicAodHideAnim",
            ),
            contains = listOf("Panoramic", "Show", "Hide", "Anim"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.controller.BaseAodController",
            source = "BaseAodController",
            category = TraceCategory.PANORAMIC,
            exact = setOf("onAodStartShow", "onAodStopShow", "startShow", "stopShow"),
            contains = listOf("AodStart", "AodStop"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.lifecycle.AodLifecycle",
            source = "AodLifecycle",
            category = TraceCategory.PANORAMIC,
            contains = listOf("Aod", "dispatch", "Start", "Stop", "Show", "Hide"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.aodclock.off.AodBlackLayout",
            source = "AodBlackLayout",
            category = TraceCategory.PANORAMIC,
            exact = setOf("onInvalidated", "onDescendantInvalidated", "onVisibilityChanged"),
            contains = listOf("Invalidat", "Visibility"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.display.SmoothTransitionController",
            source = "SmoothTransitionController",
            category = TraceCategory.PANORAMIC,
            exact = setOf(
                "updateCurrentUiState",
                "smoothTransitionRequestScreenState",
                "requestScreenStateForSysUiComponent",
            ),
            contains = listOf("Transition", "ScreenState", "UiState"),
            forceStack = true,
        )
    }

    private fun installRules(runtime: HookRuntime) {
        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.aodclock.off.AodUpdateManager",
            source = "AodUpdateManager",
            category = TraceCategory.RULES,
            exact = setOf("needDisplayAodInSpecialRule", "setIsHideBySpecialRule"),
            prefixes = listOf("hideAod", "updateAod", "setIsHide", "needDisplay"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.aodclock.off.AodUpdateManager\$2",
            source = "AodUpdateManagerSensor",
            category = TraceCategory.RULES,
            exact = setOf("hideAodByDarkLight"),
            forceStack = true,
        )
    }

    private fun installWake(runtime: HookRuntime) {
        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.display.OplusWakeUpController\$AodSingleClickWakeUpCallback",
            source = "AodSingleClickWakeUpCallback",
            category = TraceCategory.WAKE,
            exact = setOf("onClick"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.aod.display.OplusWakeUpController",
            source = "OplusWakeUpController",
            category = TraceCategory.WAKE,
            contains = listOf("Wake", "Click", "Touch", "DoubleTap", "SingleClick"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech",
            source = "OnScreenFingerprintUiMech",
            category = TraceCategory.WAKE,
            exact = setOf("notifyHideAodIcon", "fpIconShow"),
            contains = listOf("Aod", "Fingerprint", "Hide", "Show"),
            forceStack = true,
        )

        TraceMethodInterceptor.install(
            runtime = runtime,
            className = "com.oplus.systemui.biometrics.OplusBiometricAuthController",
            source = "OplusBiometricAuthController",
            category = TraceCategory.WAKE,
            contains = listOf("Udfps", "Fingerprint", "Aod", "Wake", "Hide"),
            forceStack = true,
        )
    }
}
