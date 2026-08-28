# Puzzle keep rules
-keepattributes Signature, *Annotation*

# Glide
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule

# kotlinx coroutines (if used)
-dontwarn kotlinx.coroutines.**

-keepattributes InnerClasses