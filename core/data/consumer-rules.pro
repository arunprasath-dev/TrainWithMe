# Keep kotlinx.serialization-generated serializers for the plan payload.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.trainwithme.core.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
