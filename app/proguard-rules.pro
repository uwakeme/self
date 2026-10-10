# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# 维护说明（2026-10 审查精简）：
# 本文件原先对 dagger.hilt / javax.inject / androidx.room / androidx.work /
# androidx.compose.runtime / androidx.datastore.preferences 六家做了整包 `-keep`，
# 等于关掉 R8 对它们的一切优化。其中 Room / WorkManager / Hilt 的关键类靠各自
# AAR 内置的 consumer 规则（proguard.txt）保住，其余经字节码扫描确认没有任何
# `Class.forName` 反射入口。
#
# 加整包 keep 之前先做一步：解开 ~/.gradle/caches/modules-2 下的对应 AAR，
# 看有没有 proguard.txt 或 META-INF/proguard/*.pro，别凭印象加。

############################################
# Hilt
############################################
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# 原先这里有 `-keep class dagger.hilt.** { *; }`，已删：
# hilt-android 的 consumer 规则已 keep 住 @EntryPoint 系列，而聚合依赖
# （hilt_aggregated_deps / aggregatedroot.codegen）是编译期读注解、运行期直引用的，
# 字节码里找不到 forName / loadClass，不需要整包保。

############################################
# Room
############################################
# 关键约束：Room.getGeneratedImplementation() 是用 Class.forName("<数据库类名>_Impl")
# 反射取实现类的，所以 `_Impl` 的类名不能被混淆。这条由 androidx.room 的
# consumer 规则 `-keep class * extends androidx.room.RoomDatabase` 兜住，
# （_Impl 继承 RoomDatabase）——不要因为"Room 自带规则"就手贱删掉下面三条注解 keep。
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

############################################
# WorkManager + Hilt Worker
############################################
# Worker 由 WorkManager 用 Class.forName(workerClassName) 反射实例化，类名必须保住。
# androidx.work 的 consumer 规则已经 keep 了
#   -keep class * extends androidx.work.Worker
#   -keep public class * extends androidx.work.ListenableWorker { public <init>(...); }
# 下面两条与之重叠，留着做冗余保险：只命中本项目自己的 Worker，体积代价可忽略。
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

# 业务领域模型：备份导出/导入按字段名反射读写，枚举也靠上面那条保 values()/valueOf()
# （R8 不接受 `-keep enum class` 这种 enum 修饰符，`**` 通配符已经覆盖全部枚举）
-keep class com.self.profile.domain.model.** { *; }

############################################
# Compose
############################################
# 原先这里有 `-keep class androidx.compose.runtime.** { *; }`，已删：
# Compose 官方不要求任何 keep（AS 的 Compose 模板 proguard-rules.pro 就是空的），
# 全量扫描 compose runtime 的 class 常量池，唯一用到反射的是
# androidx.compose.runtime.reflect.ComposableMethodKt（本项目未使用）。
# 只留 dontwarn，避免库内可选依赖触发 R8 警告。
-dontwarn androidx.compose.**

############################################
# 农历库（反射）
############################################
-keep class com.nlf.calendar.** { *; }

############################################
# 资源引用（部分场景会被 shrink 误删）
############################################
# 原先这里有 `-keepclassmembers class **.R$* { public static <fields>; }`，已删：
# nonTransitiveRClass=true 时 R 类按模块拆包，`**.R$*` 对 app 自己的
# com.self.profile.R$* 不是精确目标，只能兜住别的模块的 R。
# A/B 实测（2026-10）：留着这条 release APK 反而大 32768 字节（一个 dex 对齐单位），
# 而资源可达性分析由 optimizeReleaseResources + shrinkResources 负责，不依赖 R 字段 keep。
-keep class com.self.profile.R$drawable { *; }
-keep class com.self.profile.R$mipmap { *; }