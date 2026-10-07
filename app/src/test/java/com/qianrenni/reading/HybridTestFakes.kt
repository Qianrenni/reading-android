package com.qianrenni.reading

import com.qianrenni.reading.data.model.AppBundleInfo
import com.qianrenni.reading.data.model.InstalledBundle
import com.qianrenni.reading.data.remote.BundleApi
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.repository.BundleInstallResult
import com.qianrenni.reading.data.repository.FileDownloader
import com.qianrenni.reading.data.repository.HybridBundleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** 便捷构造测试用页面包信息。 */
fun testBundleInfo(
    appKey: String = "demo",
    route: String = "hybrid-demo-h5",
    engine: String = "h5",
    entry: String = "index.html",
    versionCode: Int = 1,
    versionName: String = "",
    md5: String = "",
    size: Long = 0,
    minAppVersion: Int = 0,
    url: String = "https://example.com/bundle.zip",
) = AppBundleInfo(
    appKey = appKey,
    route = route,
    engine = engine,
    entry = entry,
    versionCode = versionCode,
    versionName = versionName,
    md5 = md5,
    size = size,
    minAppVersion = minAppVersion,
    url = url,
)

/** 便捷构造测试用已安装包。 */
fun testInstalledBundle(
    appKey: String = "demo",
    route: String = "hybrid-demo-h5",
    engine: String = "h5",
    entry: String = "index.html",
    versionCode: Int = 1,
    filePath: String = "/tmp/does-not-exist",
    installedAt: Long = 0,
) = InstalledBundle(
    appKey = appKey,
    route = route,
    engine = engine,
    entry = entry,
    versionCode = versionCode,
    filePath = filePath,
    installedAt = installedAt,
)

/** 内存版页面包 API：记录 info 调用，便于断言「扫到包才请求」。 */
open class FakeBundleApi : BundleApi {
    var activeResult: NetworkResult<AppBundleInfo> = NetworkResult.Failure("n/a")
    var infoResult: NetworkResult<AppBundleInfo> = NetworkResult.Failure("n/a")

    val infoCalls = mutableListOf<Pair<String, Int>>()

    override suspend fun active(appKey: String): NetworkResult<AppBundleInfo> = activeResult

    override suspend fun info(appKey: String, versionCode: Int): NetworkResult<AppBundleInfo> {
        infoCalls += appKey to versionCode
        return infoResult
    }
}

/**
 * 内存版下载器：按 [content] 伪造下载结果，可注入 [error] 模拟网络失败。
 *
 * 进度回调刻意发两次（下载中 / 完成），用来验证仓库是否真的把进度透传给 UI。
 */
open class FakeFileDownloader(
    var content: ByteArray = "package".toByteArray(),
    var error: Exception? = null,
) : FileDownloader {
    var downloadCount = 0
    val progressValues = mutableListOf<Float>()

    override suspend fun download(url: String, target: File, onProgress: (Float) -> Unit) {
        downloadCount++
        onProgress(0.5f)
        progressValues += 0.5f
        error?.let { throw it }
        target.parentFile?.mkdirs()
        target.writeBytes(content)
        onProgress(1f)
        progressValues += 1f
    }
}

/** 内存版页面包仓库：ViewModel 测试只关心「是否发起安装、结果如何」。 */
open class FakeHybridBundleRepository : HybridBundleRepository {
    private val _installed = MutableStateFlow<List<InstalledBundle>>(emptyList())
    override val installed: StateFlow<List<InstalledBundle>> = _installed

    var installResult: BundleInstallResult = BundleInstallResult.Installed(testInstalledBundle())
    val installCalls = mutableListOf<AppBundleInfo>()

    /** 预置已安装包（模拟「上次扫码装过」）。 */
    fun seed(bundles: List<InstalledBundle>) {
        _installed.value = bundles
    }

    override fun installedFor(route: String): InstalledBundle? =
        _installed.value.filter { it.route == route }.maxByOrNull { it.versionCode }

    override suspend fun install(
        info: AppBundleInfo,
        onProgress: (Float) -> Unit,
    ): BundleInstallResult {
        installCalls += info
        onProgress(1f)
        return installResult
    }

    override suspend fun fetchActive(appKey: String): AppBundleInfo? = null

    override suspend fun uninstall(route: String) {
        _installed.value = _installed.value.filterNot { it.route == route }
    }
}
