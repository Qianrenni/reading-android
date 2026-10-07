package com.qianrenni.reading.data.repository

import com.qianrenni.reading.FakeBundleApi
import com.qianrenni.reading.FakeFileDownloader
import com.qianrenni.reading.testBundleInfo
import com.qianrenni.reading.data.remote.NetworkResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

/**
 * 页面包仓库测试：下载 → 校验 → 原子落盘 → 清单持久化 → 回退清理。
 *
 * 用真实文件系统（临时目录）+ 假下载器，覆盖「包损坏/被替换/版本要求不满足时不落清单」
 * 这条安全线——这是容器里唯一会执行外部代码的地方，校验必须真的挡住。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HybridBundleRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val content = "package-body-v1".toByteArray()
    private val md5 = md5Of(content)

    private fun md5Of(bytes: ByteArray): String =
        MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun repository(
        downloader: FakeFileDownloader = FakeFileDownloader(content = content),
        api: FakeBundleApi = FakeBundleApi(),
        appVersionCode: Int = 100,
    ) = HybridBundleRepositoryImpl(
        bundleApi = api,
        downloader = downloader,
        rootDir = temp.root,
        appVersionCodeProvider = { appVersionCode },
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private fun validInfo(versionCode: Int = 1) = testBundleInfo(
        versionCode = versionCode,
        md5 = md5,
        size = content.size.toLong(),
    )

    @Test
    fun `安装成功会落盘并写入清单`() = runTest {
        val repo = repository()

        val result = repo.install(validInfo())

        assertTrue(result is BundleInstallResult.Installed)
        val bundle = (result as BundleInstallResult.Installed).bundle
        assertEquals(File(temp.root, "demo/1/bundle.html").absolutePath, bundle.filePath)
        assertEquals("package-body-v1", File(bundle.filePath).readText())
        assertEquals(md5, bundle.md5)
        assertEquals(listOf(1), repo.installed.value.map { it.versionCode })
        assertEquals(1, repo.installedFor("hybrid-demo-h5")?.versionCode)
    }

    @Test
    fun `清单持久化后新实例（模拟重启）能读回来`() = runTest {
        repository().install(validInfo(versionCode = 4))

        val reopened = repository()

        assertEquals(listOf(4), reopened.installed.value.map { it.versionCode })
        assertEquals("hybrid-demo-h5", reopened.installed.value.single().route)
    }

    @Test
    fun `校验和不匹配时拒绝安装且不写清单`() = runTest {
        val repo = repository()

        val result = repo.install(testBundleInfo(md5 = "f".repeat(32), size = content.size.toLong()))

        assertTrue(result is BundleInstallResult.Failed)
        assertTrue((result as BundleInstallResult.Failed).message.contains("校验"))
        assertTrue(repo.installed.value.isEmpty())
        assertFalse(File(temp.root, "demo/1/bundle.html").exists())
    }

    @Test
    fun `体积与服务器不一致时拒绝安装`() = runTest {
        val repo = repository()

        val result = repo.install(testBundleInfo(md5 = md5, size = content.size + 100L))

        assertTrue(result is BundleInstallResult.Failed)
        assertTrue((result as BundleInstallResult.Failed).message.contains("大小"))
        assertTrue(repo.installed.value.isEmpty())
    }

    @Test
    fun `客户端版本过低时在下载前就拒绝`() = runTest {
        val downloader = FakeFileDownloader(content = content)
        val repo = repository(downloader = downloader, appVersionCode = 10)

        val result = repo.install(validInfo().copy(minAppVersion = 20))

        assertTrue(result is BundleInstallResult.Failed)
        assertTrue((result as BundleInstallResult.Failed).message.contains("升级"))
        assertEquals(0, downloader.downloadCount)
        assertTrue(repo.installed.value.isEmpty())
    }

    @Test
    fun `下载失败时提示网络原因且不写清单`() = runTest {
        val downloader = FakeFileDownloader(content = content, error = IllegalStateException("timeout"))
        val repo = repository(downloader = downloader)

        val result = repo.install(validInfo())

        assertTrue(result is BundleInstallResult.Failed)
        assertTrue((result as BundleInstallResult.Failed).message.contains("下载失败"))
        assertTrue(repo.installed.value.isEmpty())
    }

    @Test
    fun `包信息不完整时拒绝安装`() = runTest {
        val repo = repository()

        assertTrue(repo.install(validInfo().copy(url = "")) is BundleInstallResult.Failed)
        assertTrue(repo.install(validInfo().copy(engine = "")) is BundleInstallResult.Failed)
        assertTrue(repo.install(validInfo().copy(entry = "")) is BundleInstallResult.Failed)
    }

    @Test
    fun `同一路由只保留最新两个版本并清理旧文件`() = runTest {
        val repo = repository()
        repeat(3) { index -> repo.install(validInfo(versionCode = index + 1)) }

        assertEquals(listOf(2, 3), repo.installed.value.map { it.versionCode }.sorted())
        assertFalse(File(temp.root, "demo/1/bundle.html").exists())
        assertTrue(File(temp.root, "demo/3/bundle.html").exists())
    }

    @Test
    fun `重复安装同一版本不会产生重复条目`() = runTest {
        val repo = repository()

        repo.install(validInfo(versionCode = 5))
        repo.install(validInfo(versionCode = 5))

        assertEquals(listOf(5), repo.installed.value.map { it.versionCode })
    }

    @Test
    fun `卸载会删除本地文件与清单条目`() = runTest {
        val repo = repository()
        val installed = repo.install(validInfo()) as BundleInstallResult.Installed

        repo.uninstall("hybrid-demo-h5")

        assertTrue(repo.installed.value.isEmpty())
        assertFalse(File(installed.bundle.filePath).exists())
        assertTrue(repository().installed.value.isEmpty())
        assertNull(repo.installedFor("hybrid-demo-h5"))
    }

    @Test
    fun `下载进度会透传给调用方`() = runTest {
        val repo = repository()
        val progress = mutableListOf<Float>()

        repo.install(validInfo(), onProgress = { progress += it })

        assertEquals(listOf(0.5f, 1f), progress)
    }

    @Test
    fun `fetchActive 透传接口结果`() = runTest {
        val api = FakeBundleApi().apply {
            activeResult = NetworkResult.Success(testBundleInfo(versionCode = 8))
        }
        val repo = repository(api = api)

        assertEquals(8, repo.fetchActive("demo")?.versionCode)

        api.activeResult = NetworkResult.Failure("404")
        assertNull(repo.fetchActive("demo"))
    }

    @Test
    fun `不同 appKey 的包互不影响`() = runTest {
        val repo = repository()

        repo.install(validInfo(versionCode = 1))
        repo.install(
            testBundleInfo(
                appKey = "other",
                route = "other-route",
                versionCode = 1,
                md5 = md5,
                size = content.size.toLong(),
            )
        )

        assertEquals(2, repo.installed.value.size)
        assertEquals(
            File(temp.root, "other/1/bundle.html").absolutePath,
            repo.installed.value.single { it.appKey == "other" }.filePath,
        )
    }
}
