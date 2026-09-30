-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

-keep class com.ayc.coloraod.trace.config.** { *; }
-keep class com.ayc.coloraod.trace.hook.TraceConfigReader { *; }
