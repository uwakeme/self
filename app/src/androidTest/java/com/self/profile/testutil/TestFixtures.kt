package com.self.profile.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.self.profile.data.local.AppDatabase

/**
 * androidTest 公共夹具。
 *
 * 全部手工构造依赖，**不引入 Hilt**：本轮四类契约要的对象（DAO / Repository /
 * DataStore / WorkManager）都能自己 new 出来，加一套 `@HiltAndroidTest` +
 * 自定义 runner 只会平白改变整个 androidTest 的运行方式。
 */
fun targetContext(): Context = InstrumentationRegistry.getInstrumentation().targetContext

/**
 * in-memory 数据库，每个用例各自建各自关。
 *
 * [Room.Builder.allowMainThreadQueries] 只在这里开，生产 builder
 * （`di/DatabaseModule.kt`）保持原样不被污染。
 */
fun createInMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(targetContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()