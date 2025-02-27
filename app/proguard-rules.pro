# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class org.pytorch.Module { *; }
-keep class org.pytorch.IValue { *; }
-keep class org.pytorch.Tensor { *; }
-keep class org.pytorch.torchvision.TensorImageUtils { *; }
-dontwarn org.pytorch.**

# Current ProGuard rule
-dontwarn org.pytorch.**

# Add more aggressive optimization 
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-keepattributes *Annotation*

# Remove Android logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Add to proguard-rules.pro
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class org.pytorch.** { *; }
-dontwarn org.pytorch.**
-keep class com.facebook.soloader.** { *; }

# Remove all logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Enable R8 full mode optimizations
-allowaccessmodification
-repackageclasses
-optimizations !code/allocation/variable

# Keep signature related classes
-keep class android.content.pm.** { *; }
-keep class android.content.Signature { *; }

# Keep certificate information
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses