package com.ayc.coloraod.trace.hook

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method

internal class HookRuntime(
    val module: XposedModule,
    val classLoader: ClassLoader,
) {
    fun findClass(name: String): Class<*> = Class.forName(name, false, classLoader)

    fun intercept(
        id: String,
        method: Method,
        block: (XposedInterface.Chain) -> Any?,
    ) = module.hook(method)
        .setId(id)
        .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
        .intercept(block)
}
