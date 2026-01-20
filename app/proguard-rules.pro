# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\urooj\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# and each project's build.gradle file.

# Keep TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-keep public class com.google.android.gms.tflite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.task.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }

# Keep ML Kit Text Recognition
-keep class com.google.mlkit.vision.text.** { *; }

# Keep Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler {
    <init>(...);
}

# Keep Hilt/Dagger
-keep class com.haramshield.HaramShieldApp { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class javax.annotation.** { *; }

# Keep Timber
-dontwarn timber.log.Timber

# Keep GSON/Serialization if used (though we use Room/DataStore)
