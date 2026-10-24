# TDLib JNI bridge
-keep class dev.g000sha256.tdl.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# NextPlayer FFmpeg decoders (reflection / JNI)
-keep class io.github.anilbeesetti.nextlib.** { *; }

-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.jetbrains.annotations.**
