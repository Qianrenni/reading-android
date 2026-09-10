package com.qianrenni.reading.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/** 应用版本信息。 */
data class AppVersion(val versionCode: Long, val versionName: String)

/** APK 文件内声明的包信息。 */
data class ApkInfo(val packageName: String, val versionCode: Long, val versionName: String)

/** 检查更新结果。 */
sealed interface UpdateCheckResult {
    /** 已是最新版本。 */
    data class UpToDate(val current: AppVersion) : UpdateCheckResult

    /** 发现新版本，安装包已下载完成。 */
    data class UpdateAvailable(val current: AppVersion, val remote: AppVersion) : UpdateCheckResult

    /** 检查失败（网络异常、安装包无法解析等）。 */
    data class Failed(val message: String) : UpdateCheckResult
}

/** 下载远端文件到本地（可注入，便于单元测试）。 */
fun interface FileDownloader {
    suspend fun download(url: String, target: File, onProgress: (Float) -> Unit)
}

/** 解析 APK 文件（可注入，便于单元测试）。返回 null 表示无法解析。 */
fun interface ApkReader {
    fun read(apkFile: File): ApkInfo?
}

/**
 * 应用更新仓库。
 *
 * 更新包由后端静态目录提供（`{baseUrl}static/guga.apk`），因此无需后端新增接口：
 * 下载安装包后直接读取 APK 内声明的 versionCode 与本地已安装版本比较。
 * 由于安装时还需要这个文件，检查阶段保留下载结果，用户确认后即可直接安装，避免二次下载。
 */
interface AppUpdateRepository {
    /** 当前已安装版本。 */
    fun currentVersion(): AppVersion

    /** 更新包地址（跟随应用内服务器配置动态变化）。 */
    fun apkUrl(): String

    /**
     * 下载更新包并解析版本号，与当前版本比较。
     *
     * @param onProgress 下载进度回调（0f..1f）
     */
    suspend fun checkForUpdate(onProgress: (Float) -> Unit = {}): UpdateCheckResult

    /** 检查通过后保留下来的安装包；不存在时返回 null。 */
    fun downloadedApk(): File?

    /** 丢弃已下载的安装包（例如用户取消更新）。 */
    fun clearDownloadedApk()
}

class AppUpdateRepositoryImpl(
    private val currentVersionProvider: () -> AppVersion,
    private val apkUrlProvider: () -> String,
    private val apkFileProvider: () -> File,
    private val downloader: FileDownloader,
    private val apkReader: ApkReader,
    /** 期望的包名，用于防止安装到不匹配的安装包；为空表示不校验。 */
    private val expectedPackageName: String = "",
    private val ioDispatcher: CoroutineDispatcher,
) : AppUpdateRepository {

    override fun currentVersion(): AppVersion = currentVersionProvider()

    override fun apkUrl(): String = apkUrlProvider()

    override fun downloadedApk(): File? = apkFileProvider().takeIf { it.exists() && it.length() > 0 }

    override fun clearDownloadedApk() {
        apkFileProvider().takeIf { it.exists() }?.delete()
    }

    override suspend fun checkForUpdate(onProgress: (Float) -> Unit): UpdateCheckResult =
        withContext(ioDispatcher) {
            val current = currentVersionProvider()
            val target = apkFileProvider()
            // 每次都重新下载，避免残留的旧安装包导致误判
            clearDownloadedApk()
            try {
                downloader.download(apkUrlProvider(), target, onProgress)
            } catch (e: Exception) {
                clearDownloadedApk()
                return@withContext UpdateCheckResult.Failed("下载更新包失败：${e.message ?: "网络异常"}")
            }

            val remote = apkReader.read(target)
            if (remote == null) {
                clearDownloadedApk()
                return@withContext UpdateCheckResult.Failed("更新包无法解析，请稍后重试")
            }
            if (expectedPackageName.isNotEmpty() && remote.packageName != expectedPackageName) {
                clearDownloadedApk()
                return@withContext UpdateCheckResult.Failed("更新包与当前应用不匹配")
            }

            if (remote.versionCode > current.versionCode) {
                UpdateCheckResult.UpdateAvailable(
                    current = current,
                    remote = AppVersion(remote.versionCode, remote.versionName)
                )
            } else {
                // 已是最新，安装包没有保留价值
                clearDownloadedApk()
                UpdateCheckResult.UpToDate(current)
            }
        }
}

/**
 * 基于 Ktor 的流式下载实现（复用无认证的裸客户端，不携带令牌）。
 */
class KtorFileDownloader(private val client: HttpClient) : FileDownloader {

    override suspend fun download(url: String, target: File, onProgress: (Float) -> Unit) {
        target.parentFile?.mkdirs()
        client.prepareGet(url).execute { response ->
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
            val channel: ByteReadChannel = response.bodyAsChannel()
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            var downloaded = 0L
            target.outputStream().use { output ->
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (contentLength > 0) {
                        onProgress((downloaded.toDouble() / contentLength).toFloat().coerceIn(0f, 1f))
                    }
                }
            }
        }
    }

    private companion object {
        const val DOWNLOAD_BUFFER_SIZE = 16 * 1024
    }
}
