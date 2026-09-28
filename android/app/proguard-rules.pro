# R8 rules for the release build. Hilt, Firebase, AndroidX, Coil, kotlinx-serialization and
# Ktor ship their own consumer rules; these cover what they don't.

# Keep generic signatures and annotations: supabase-kt and kotlinx-serialization look up
# serializers from reified types (decodeList<T>(), decodeAs<T>()) at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, Exceptions

# kotlinx-serialization: this app's @Serializable DTOs, navigation routes and offline cache.
-keepclassmembers @kotlinx.serialization.Serializable class app.twoverse.** {
    static **$Companion Companion;
    static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.twoverse.**$$serializer { *; }

# supabase-kt ships no rules. Its request and Realtime message classes are serialized by
# name, and its plugins are looked up by key, so its model classes keep their names.
-keep,includedescriptorclasses class io.github.jan.supabase.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class io.github.jan.supabase.** {
    static **$Companion Companion;
    static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepnames class io.github.jan.supabase.**

# Ktor: optional JVM-only classes that are never used on Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

# Credential Manager finds its Play services implementation by reflection.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
