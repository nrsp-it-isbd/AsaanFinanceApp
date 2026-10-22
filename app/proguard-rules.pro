# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data classes used with Gson
-keepclassmembers class com.assan.finance.model.** {
    <fields>;
    <init>(...);
}

# Keep Retrofit interfaces
-keep interface com.assan.finance.network.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Coil
-dontwarn coil.**

# RootBeer
-keep class com.scottyab.rootbeer.** { *; }

# SignalR
-keep class com.microsoft.signalr.** { *; }

# Security Crypto
-keep class androidx.security.crypto.** { *; }

# FIX: Gson response/data models outside com.assan.finance.model were not protected.
# With R8 (minifyEnabled true) their fields can be renamed/stripped, so release builds
# parse API responses (loan submit, home data, products, banks...) as empty/null.
-keep class com.assan.finance.responseModel.** { *; }
-keep class com.assan.finance.dataModels.** { *; }
-keep class com.assan.finance.model.** { *; }
-keep class com.assan.finance.requestModels.** { *; }

# SignalR pulls in slf4j without a logger binding; the binding is optional at runtime.
# Without this R8 stops the release build with "Missing class org.slf4j.impl.StaticLoggerBinder".
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Release builds: remove android.util.Log calls so CNIC numbers, tokens and form data
# never reach logcat (Play / SECP data-protection). Debug builds keep their logs.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static boolean isLoggable(java.lang.String, int);
}
