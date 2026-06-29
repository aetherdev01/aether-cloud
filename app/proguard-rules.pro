# ═══════════════════════════════════════════════════════════════════════════════
# AETHER CLOUD — ProGuard / R8 Rules
# ═══════════════════════════════════════════════════════════════════════════════

# ── R8 Optimisasi ─────────────────────────────────────────────────────────────
-repackageclasses ''
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5
-optimizations !code/simplification/cast,field/*,class/merging/*,code/allocation/variable

# ── Attributes wajib ──────────────────────────────────────────────────────────
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# =============================================================================
# AETHER CLOUD — CORE
# =============================================================================
-keep public class com.aether.cloud.MainActivity { <init>(); }
-keep public class com.aether.cloud.AetherApp { <init>(); }

# =============================================================================
# KOTLIN
# =============================================================================
-keep class kotlin.Metadata { *; }
-keepclassmembernames class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
    public static void parameter*(...);
}

# =============================================================================
# JETPACK COMPOSE
# =============================================================================
-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(...);
    void sourceInformationMarkerStart(...);
    void sourceInformationMarkerEnd(...);
    boolean isTraceInProgress();
    void traceEventStart(...);
    void traceEventEnd();
}

# =============================================================================
# ROOM
# =============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# =============================================================================
# DATASTORE
# =============================================================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# =============================================================================
# GSON
# =============================================================================
-keep class com.google.gson.** { *; }
-keep class com.aether.cloud.data.model.** { *; }
-dontwarn com.google.gson.**

# =============================================================================
# FIREBASE
# =============================================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# =============================================================================
# FACEBOOK LOGIN
# =============================================================================
-keep class com.facebook.** { *; }
-keep interface com.facebook.** { *; }
-keep enum com.facebook.** { *; }
-dontwarn com.facebook.**

# =============================================================================
# COIL / GLIDE
# =============================================================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.**
-dontwarn io.coil.kt.**

# =============================================================================
# UNITY ADS
# =============================================================================
-keep class com.unity3d.** { *; }
-keep interface com.unity3d.** { *; }
-keep class com.unity3d.services.** { *; }
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.**

# =============================================================================
# PARCELABLE / SERIALIZABLE / ENUM
# =============================================================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public final ** name();
    public final int ordinal();
}

# =============================================================================
# SUPPRESS WARNINGS
# =============================================================================
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
