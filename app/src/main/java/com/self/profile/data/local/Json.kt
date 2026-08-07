package com.self.profile.data.local

import kotlinx.serialization.json.Json

/**
 * 全局 JSON 实例。忽略未知字段（向前兼容 schema 演进），允许宽松解析。
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
