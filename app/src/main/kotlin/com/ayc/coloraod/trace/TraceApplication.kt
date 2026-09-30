package com.ayc.coloraod.trace

import android.app.Application
import com.ayc.coloraod.trace.config.TraceConfigStore
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.atomic.AtomicReference

class TraceApplication : Application(), XposedServiceHelper.OnServiceListener {
    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        serviceRef.set(service)
        TraceConfigStore.onXposedServiceBound(this, service)
    }

    override fun onServiceDied(service: XposedService) {
        serviceRef.compareAndSet(service, null)
    }

    companion object {
        private val serviceRef = AtomicReference<XposedService?>(null)

        internal fun service(): XposedService? = serviceRef.get()
        fun isServiceConnected(): Boolean = serviceRef.get() != null
    }
}
