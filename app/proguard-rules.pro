# ======= Attributes & Kotlin metadata =======
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-keepclassmembers class kotlin.Metadata { *; }

# ======= Hilt / Dagger (محدد ومأمون) =======
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Keep generated Dagger classes that follow typical naming
-keep class *Dagger* { *; }

# ======= Room (محدد على Entities وDAOs وDatabase) =======
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ======= Jetpack Compose (محدد) =======
# Keep annotation & compiler metadata required by Compose runtime
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
# Keep methods annotated with @Composable (if any)
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
# Keep Compose runtime entrypoints used by framework (limit to runtime package)
-keep class androidx.compose.runtime.** { *; }

# ======= Billing (محدد) =======
-keep class com.android.billingclient.** { public *; }
-dontwarn com.android.billingclient.**

# ======= AdMob / Play Services (محدد) =======
-keep class com.google.android.gms.ads.** { public *; }
-dontwarn com.google.android.gms.ads.**

# ======= Coroutines (بسيط) =======
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.* { *; }

# ======= Gson / Serialization (محدد للحزم الخاصة بك) =======
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.ahmedgamal.aquamemo.data.model.** { *; }

# ======= Serializable support =======
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ======= Crashlytics =======
-keepattributes SourceFile,LineNumberTable
-dontwarn com.google.firebase.crashlytics.**
-keep class com.google.firebase.crashlytics.** { *; }

# ======= Misc warnings suppression =======
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn kotlin.reflect.jvm.internal.impl.**
