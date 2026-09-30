package com.ayc.coloraod.trace

import android.util.Log
import com.ayc.coloraod.trace.config.TraceConfigStore
import com.ayc.coloraod.trace.hook.HookRuntime
import com.ayc.coloraod.trace.hook.TraceConfigReader
import com.ayc.coloraod.trace.hook.TraceInstaller
import com.ayc.coloraod.trace.hook.TraceLog
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class ModuleEntry : XposedModule() {
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        val message = "process=${param.processName} api=${getApiVersion()} framework=${getFrameworkName()}"
        log(Log.INFO, TraceLog.TAG, "MODULE_LOADED: $message")
        Log.i(TraceLog.TAG, "MODULE_LOADED: $message")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName != SYSTEM_UI) return

        TraceConfigReader.bindPrefs(getRemotePreferences(TraceConfigStore.PREFS_NAME))
        val runtime = HookRuntime(this, param.classLoader)
        TraceLog.i("PACKAGE_READY", "package=${param.packageName}")

        runCatching {
            TraceInstaller.install(runtime)
        }.onSuccess {
            TraceLog.i("INSTALL_COMPLETE", "mode=passive-read-only")
        }.onFailure {
            TraceLog.e("INSTALL_FAILED", "trace installer failed", it)
        }
    }

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
    }
}
