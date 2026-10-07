package com.qianrenni.reading.data.repository

import com.qianrenni.reading.data.model.AppBundleInfo
import com.qianrenni.reading.data.model.InstalledBundle
import com.qianrenni.reading.data.remote.BundleApi
import com.qianrenni.reading.data.remote.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/** 安装结果。 */
sealed interface BundleInstallResult {
    data class Installed(val bundle: InstalledBundle) : BundleInstallResult
    data class Failed(val message: String) : BundleInstallResult
}

/**
 * 容器页面包仓库：负责下载、校验、安装、查询与回退。
 *
 * 存储布局（应用私有目录，不受外部影响）：
 * ```
 * filesDir/hybrid/index.json                    已安装清单
 * filesDir/hybrid/{appKey}/{versionCode}/bundle.{ext}
 * ```
 *
 * 安装顺序刻意是「先下载到临时文件 → 校验 MD5 → 原子移入版本目录 → 最后原子改写清单」：
 * 任何一步失败都不会让清单指向一个不完整/被篡改的文件，最坏情况只是留下一个无人引用的临时文件。
 */
interface HybridBundleRepository {
    /** 本机已安装的包（按 route 去重后包含各版本，最新在前）。 */
    val installed: StateFlow<List<InstalledBundle>>

    /** 某个 route 当前应使用的包（版本号最高的已安装包）。 */
    fun installedFor(route: String): InstalledBundle?

    /**
     * 按服务端下发的信息安装指定版本。
     *
     * @param onProgress 下载进度（0f..1f）
     */
    suspend fun install(
        info: AppBundleInfo,
        onProgress: (Float) -> Unit = {}
    ): BundleInstallResult

    /** 拉取 appKey 的当前生效版本；失败或无版本时返回 null。 */
    suspend fun fetchActive(appKey: String): AppBundleInfo?

    /** 卸载某个 route 的全部本地包（调试/回退用）。 */
    suspend fun uninstall(route: String)
}

class HybridBundleRepositoryImpl(
    private val bundleApi: BundleApi,
    private val downloader: FileDownloader,
    private val rootDir: File,
    private val appVersionCodeProvider: () -> Int,
    private val ioDispatcher: CoroutineDispatcher,
) : HybridBundleRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val mutex = Mutex()
    private val indexFile: File get() = File(rootDir, INDEX_FILE_NAME)

    private val _installed = MutableStateFlow(loadIndex())
    override val installed: StateFlow<List<InstalledBundle>> = _installed.asStateFlow()

    override fun installedFor(route: String): InstalledBundle? =
        _installed.value.filter { it.route == route }.maxByOrNull { it.versionCode }

    override suspend fun fetchActive(appKey: String): AppBundleInfo? =
        when (val result = bundleApi.active(appKey)) {
            is NetworkResult.Success -> result.data
            else -> null
        }

    override suspend fun install(info: AppBundleInfo, onProgress: (Float) -> Unit): BundleInstallResult =
        withContext(ioDispatcher) {
            mutex.withLock {
                if (info.url.isBlank()) {
                    return@withLock BundleInstallResult.Failed("包下载地址为空")
                }
                val currentAppVersion = appVersionCodeProvider()
                if (info.minAppVersion > 0 && currentAppVersion < info.minAppVersion) {
                    return@withLock BundleInstallResult.Failed(
                        "该页面包要求客户端版本 ≥ ${info.minAppVersion}，请先升级应用"
                    )
                }
                if (info.engine.isBlank() || info.entry.isBlank()) {
                    return@withLock BundleInstallResult.Failed("包信息不完整")
                }

                val versionDir = File(rootDir, "${sanitize(info.appKey)}/${info.versionCode}")
                versionDir.mkdirs()
                val extension = info.entry.substringAfterLast('.', DEFAULT_EXTENSION)
                    .takeIf { it.length in 1..8 } ?: DEFAULT_EXTENSION
                val target = File(versionDir, "bundle.$extension")
                val temp = File(versionDir, "bundle.$extension.tmp")

                try {
                    downloader.download(info.url, temp, onProgress)
                } catch (e: Exception) {
                    temp.delete()
                    return@withLock BundleInstallResult.Failed("下载失败：${e.message ?: "网络异常"}")
                }

                // 校验：服务端声明的 MD5 与体积都要对得上，避免损坏或被替换的包进入渲染
                val actualMd5 = md5Of(temp)
                if (info.md5.isNotBlank() && !actualMd5.equals(info.md5, ignoreCase = true)) {
                    temp.delete()
                    return@withLock BundleInstallResult.Failed("包校验失败，请重新扫码下载")
                }
                if (info.size > 0 && temp.length() != info.size) {
                    temp.delete()
                    return@withLock BundleInstallResult.Failed("包大小与服务器不一致，请重新下载")
                }
                if (temp.length() == 0L) {
                    temp.delete()
                    return@withLock BundleInstallResult.Failed("包内容为空")
                }

                target.delete()
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }

                val bundle = InstalledBundle(
                    appKey = info.appKey,
                    route = info.route,
                    engine = info.engine,
                    entry = info.entry,
                    versionCode = info.versionCode,
                    versionName = info.versionName,
                    md5 = actualMd5,
                    filePath = target.absolutePath,
                    installedAt = System.currentTimeMillis()
                )
                val next = (_installed.value.filterNot {
                    it.route == bundle.route && it.versionCode == bundle.versionCode
                } + bundle)
                // 同一 route 只保留最新两个版本，支持「换版后回到上一版」
                val pruned = prune(keepPerRoute = 2, list = next)
                persistIndex(pruned)
                deleteUnreferenced(pruned)
                _installed.value = pruned
                BundleInstallResult.Installed(bundle)
            }
        }

    override suspend fun uninstall(route: String) = withContext(ioDispatcher) {
        mutex.withLock {
            val removed = _installed.value.filter { it.route == route }
            if (removed.isEmpty()) {
                return@withLock
            }
            val next = _installed.value.filterNot { it.route == route }
            persistIndex(next)
            _installed.value = next
            removed.forEach { bundle ->
                File(bundle.filePath).takeIf { it.exists() }?.delete()
            }
        }
    }

    /** 同一 route 保留（版本号最大的）[keepPerRoute] 个版本，其余丢弃。 */
    private fun prune(keepPerRoute: Int, list: List<InstalledBundle>): List<InstalledBundle> {
        val kept = mutableListOf<InstalledBundle>()
        list.groupBy { it.route }.forEach { (_, group) ->
            kept += group.sortedByDescending { it.versionCode }.take(keepPerRoute)
        }
        return kept.sortedBy { it.installedAt }
    }

    /** 删除不再被清单引用的包文件（失败只留残留文件，不影响功能）。 */
    private fun deleteUnreferenced(list: List<InstalledBundle>) {
        val referenced = list.map { it.filePath }.toSet()
        runCatching {
            rootDir.listFiles()?.forEach { appDir ->
                appDir.listFiles()?.forEach { versionDir ->
                    versionDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.absolutePath !in referenced) {
                            file.delete()
                        }
                    }
                }
            }
        }
    }

    private fun loadIndex(): List<InstalledBundle> {
        val file = indexFile
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<InstalledBundle>>(file.readText())
        }.getOrElse { emptyList() }
    }

    /** 原子改写清单：先写临时文件再改名，避免进程被杀时留下半截 JSON。 */
    private fun persistIndex(list: List<InstalledBundle>) {
        rootDir.mkdirs()
        val temp = File(rootDir, "$INDEX_FILE_NAME.tmp")
        temp.writeText(json.encodeToString(list))
        if (!temp.renameTo(indexFile)) {
            temp.copyTo(indexFile, overwrite = true)
            temp.delete()
        }
    }

    private fun sanitize(appKey: String): String =
        appKey.lowercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .ifEmpty { "default" }

    private fun md5Of(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val INDEX_FILE_NAME = "index.json"
        const val DEFAULT_EXTENSION = "bundle"
    }
}
