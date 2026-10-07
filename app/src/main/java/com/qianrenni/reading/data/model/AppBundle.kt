package com.qianrenni.reading.data.model

import kotlinx.serialization.Serializable

/**
 * 容器页面包信息（服务端下发）。
 *
 * @param entry h5：包内入口相对路径或绝对 URL；rn：注册的 component 名
 * @param url 包下载地址（由服务端按 `serverUrl` 拼好）
 * @param md5 包内容 MD5，下载后必须校验
 */
@Serializable
data class AppBundleInfo(
    val id: Int = 0,
    val appKey: String,
    val route: String,
    val engine: String,
    val entry: String,
    val versionCode: Int,
    val versionName: String = "",
    val md5: String = "",
    val size: Long = 0,
    val description: String = "",
    val isActive: Boolean = false,
    val minAppVersion: Int = 0,
    val url: String = "",
    val createdAt: String = ""
)

/** 已安装到本机的包（落盘在应用私有目录）。 */
@Serializable
data class InstalledBundle(
    val appKey: String,
    val route: String,
    val engine: String,
    val entry: String,
    val versionCode: Int,
    val versionName: String = "",
    val md5: String = "",
    /** 包文件绝对路径 */
    val filePath: String,
    val installedAt: Long = 0
)
