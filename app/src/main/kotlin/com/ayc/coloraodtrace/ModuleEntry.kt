package com.ayc.coloraodtrace

import android.util.Log
import com.ayc.coloraodtrace.hook.TraceInstaller
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class ModuleEntry : XposedModule() {
    @Volatile
    private var processName: String? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
        val message = "event=module_loaded process=${param.processName} api=${getApiVersion()} framework=${getFrameworkName()}"
        log(Log.INFO, TAG, message)
        Log.i(TAG, message)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName != SYSTEM_UI) return

        val message = "event=package_ready package=${param.packageName} process=$processName"
        log(Log.INFO, TAG, message)
        Log.i(TAG, message)

        runCatching {
            TraceInstaller.install(this, param.classLoader)
        }.onFailure {
            val error = "event=install_failed error=${it.stackTraceToString()}"
            log(Log.ERROR, TAG, error)
            Log.e(TAG, error)
        }
    }

    private companion object {
        const val TAG = "AOD_Trace"
        const val SYSTEM_UI = "com.android.systemui"
    }
}
