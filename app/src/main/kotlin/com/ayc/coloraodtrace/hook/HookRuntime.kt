package com.ayc.coloraodtrace.hook

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method

internal class HookRuntime(
    val module: XposedModule,
    val classLoader: ClassLoader,
) {
    fun findClass(name: String): Class<*> = Class.forName(name, false, classLoader)

    fun declaredMethods(className: String): List<Method> =
        findClass(className).declaredMethods.toList().onEach {
            runCatching { it.isAccessible = true }
        }

    fun intercept(
        id: String,
        method: Method,
        block: (XposedInterface.Chain) -> Any?,
    ) = module.hook(method)
        .setId(id)
        .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
        .intercept(block)
}
