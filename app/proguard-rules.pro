# ProGuard rules for SNL Demo
# Room
-keep class androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.local.snldemo.data.local.** { *; }

