# ==========================================
# Core App Keep Rules
# ==========================================

# 1. Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class com.example.data.local.** { *; }

# 2. JNI Libraries (ONNX, Llama.cpp)
# If native methods exist, preserve them so JNI works
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.microsoft.onnxruntime.** { *; }
-keep class de.kherud.llama.** { *; }
-keep class com.example.backend.LlamaCppBridge { *; }

# 3. Google ML Kit (OCR)
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.mlkit.vision.text.** { *; }

# 4. Moshi & JSON Parsing
# Keep all data classes used in Retrofit / JSON parsing
-keep class com.example.backend.SearchResult { *; }
-keep class com.example.backend.RagMetadata { *; }
-keep class kotlin.Metadata { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# 5. Coroutines & Flows
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# 6. Jetpack Compose
# Keep composables from being stripped if called dynamically (usually R8 handles this well, but safe fallback)
-keep class androidx.compose.** { *; }

# 7. OkHttp / Retrofit
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

