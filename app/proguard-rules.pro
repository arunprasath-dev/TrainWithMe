# kotlinx.serialization: keep generated serializers for the persisted plan graph.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.trainwithme.core.model.**$$serializer { *; }
-keepclassmembers class com.trainwithme.core.model.** { *** Companion; }
-keepclasseswithmembers class com.trainwithme.core.model.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class androidx.room.RoomProcessor { *; }
-dontwarn androidx.room.paging.**

# Media3 / ExoPlayer defaults are handled by the library's own rules.
-dontwarn org.checkerframework.**

# Keep line numbers for readable crash reports, hide the source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
