# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

############################################
# Hilt
############################################
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# Hilt 生成的组件（运行时反射用）
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

############################################
# Room
############################################
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

############################################
# WorkManager + Hilt Worker
############################################
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep @androidx.hilt.work.HiltWorker class * { *; }

# ReminderWorker 等用 @AssistedInject 构造，需要保留
-keepclasseswithmembers class * {
    @dagger.assisted.AssistedInject <init>(...);
}

############################################
# kotlinx.serialization
############################################
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# 保留所有 @Serializable 类（字段 + 生成的 serializer）
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# 防止 enum 序列化被破坏（values() / valueOf() 反射调用）
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# AppSettings / DarkModePref / FontSizePref / GallerySortRule / ReminderType / RepeatRule / RelationshipType / ClosenessLevel
# （** 通配符已覆盖 enum 类型；R8 不接受 `-keep enum class` 这种 `enum` 修饰符）
-keep class com.self.profile.domain.model.** { *; }

############################################
# Compose
############################################
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

############################################
# 农历库（反射）
############################################
-keep class com.nlf.calendar.** { *; }

############################################
# 资源引用（部分场景会被 shrink 误删）
############################################
-keepclassmembers class **.R$* {
    public static <fields>;
}

############################################
# DataStore（Preferences）
############################################
-keep class androidx.datastore.preferences.** { *; }

############################################
# 业务：避免 R$drawable 误删启动图 / 图标
############################################
-keep class com.self.profile.R$drawable { *; }
-keep class com.self.profile.R$mipmap { *; }
