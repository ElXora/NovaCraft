# ─── NovaCraft Launcher ProGuard Rules ──────────────────────────────────────

# Keep application class
-keep class com.novacraft.launcher.NovaCraftApp { *; }
-keep class com.novacraft.launcher.MainActivity { *; }
-keep class com.novacraft.launcher.service.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson / DTOs
-keep class com.novacraft.launcher.data.remote.dto.** { *; }
-keep class com.novacraft.launcher.domain.model.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-dontwarn com.google.gson.**

# Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Timber
-keep class timber.log.Timber { *; }

# Security crypto
-keep class androidx.security.crypto.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
