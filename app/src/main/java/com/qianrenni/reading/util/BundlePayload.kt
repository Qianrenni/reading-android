package com.qianrenni.reading.util

/**
 * 容器页面包二维码内容的解析。
 *
 * 约定（与后端 `com.qianrenni.modules.system.BundlePayload` 保持一致）：
 * `gugareading://bundle?appKey=<k>&version=<n>&md5=<m>`
 *
 * 二维码里**只有标识与校验和，没有下载地址**：真正的下载地址、引擎与入口都由服务端按
 * (appKey, version) 下发，因此陌生二维码最多让本机安装一个服务端已发布的版本，
 * 无法把客户端引导到别处去下载代码。
 *
 * 手工解析而非 `android.net.Uri`：本类要能在 JVM 单测里直接验证（`Uri` 在非仪器化测试中只有桩实现）。
 */
object BundlePayload {

    const val SCHEME = "gugareading"
    const val HOST = "bundle"
    const val PARAM_APP_KEY = "appKey"
    const val PARAM_VERSION = "version"
    const val PARAM_MD5 = "md5"

    private const val SCHEME_SEPARATOR = "://"
    private const val PARAM_SEPARATOR = '&'
    private const val VALUE_SEPARATOR = '='
    private val MD5_PATTERN = Regex("^[0-9a-fA-F]{32}$")

    // 与后端 BundleService 的 appKey 校验保持一致：小写字母开头，仅含小写字母/数字/-/_
    private val APP_KEY_PATTERN = Regex("^[a-z0-9][a-z0-9_-]{0,63}$")

    // 解析时参数名统一转小写，查找也必须用小写键（否则 appKey 永远取不到）
    private val KEY_APP_KEY = PARAM_APP_KEY.lowercase()
    private val KEY_VERSION = PARAM_VERSION.lowercase()
    private val KEY_MD5 = PARAM_MD5.lowercase()

    /** 解析结果：包标识 + 版本 + 校验和。 */
    data class Request(
        val appKey: String,
        val versionCode: Int,
        val md5: String,
    )

    fun build(appKey: String, versionCode: Int, md5: String): String =
        "$SCHEME$SCHEME_SEPARATOR$HOST?$PARAM_APP_KEY=$appKey" +
                "$PARAM_SEPARATOR$PARAM_VERSION=$versionCode" +
                "$PARAM_SEPARATOR$PARAM_MD5=$md5"

    /**
     * @return 解析结果；不是本系统的页面包二维码、或缺少必要字段时返回 null
     */
    fun parse(raw: String?): Request? {
        val text = raw?.trim().orEmpty()
        val schemeEnd = text.indexOf(SCHEME_SEPARATOR)
        if (schemeEnd <= 0) return null
        if (!text.substring(0, schemeEnd).equals(SCHEME, ignoreCase = true)) return null

        val rest = text.substring(schemeEnd + SCHEME_SEPARATOR.length)
        val queryStart = rest.indexOf('?')
        if (queryStart < 0) return null
        if (!rest.substring(0, queryStart).equals(HOST, ignoreCase = true)) return null

        val params = mutableMapOf<String, String>()
        rest.substring(queryStart + 1).split(PARAM_SEPARATOR).forEach { param ->
            val separator = param.indexOf(VALUE_SEPARATOR)
            if (separator > 0) {
                params[param.substring(0, separator).lowercase()] = param.substring(separator + 1)
            }
        }

        val appKey = params[KEY_APP_KEY]?.trim()?.lowercase()?.takeIf { APP_KEY_PATTERN.matches(it) }
            ?: return null
        val versionCode = params[KEY_VERSION]?.trim()?.toIntOrNull()?.takeIf { it > 0 } ?: return null
        val md5 = params[KEY_MD5]?.trim()?.takeIf { MD5_PATTERN.matches(it) } ?: return null
        return Request(appKey = appKey, versionCode = versionCode, md5 = md5)
    }
}
