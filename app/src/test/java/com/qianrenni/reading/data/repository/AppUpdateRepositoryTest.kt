package com.qianrenni.reading.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

private const val TEST_PACKAGE = "com.qianrenni.reading"

class AppUpdateRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val installed = AppVersion(versionCode = 5, versionName = "1.0.5")

    private val apkFile: File
        get() = File(tempFolder.root, "app_update/guga.apk")

    /** 模拟下载：写出文件并回调进度。 */
    private fun downloader(
        onDownload: (File) -> Unit = { it.writeText("apk-bytes") }
    ) = FileDownloader { _, target, onProgress ->
        target.parentFile?.mkdirs()
        onProgress(0.5f)
        onDownload(target)
        onProgress(1f)
    }

    private fun repository(
        remote: ApkInfo? = ApkInfo(TEST_PACKAGE, versionCode = 6, versionName = "1.0.6"),
        downloader: FileDownloader = downloader(),
        expectedPackage: String = TEST_PACKAGE,
    ) = AppUpdateRepositoryImpl(
        currentVersionProvider = { installed },
        apkUrlProvider = { "http://example.com/static/guga.apk" },
        apkFileProvider = { apkFile },
        downloader = downloader,
        apkReader = { remote },
        expectedPackageName = expectedPackage,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `apk url follows configured base url`() {
        assertEquals("http://example.com/static/guga.apk", repository().apkUrl())
    }

    @Test
    fun `reports update available and keeps apk for install`() = runTest {
        val repo = repository()

        val result = repo.checkForUpdate()

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val available = result as UpdateCheckResult.UpdateAvailable
        assertEquals("1.0.6", available.remote.versionName)
        assertEquals(6L, available.remote.versionCode)
        // 有新版本时保留下载结果，安装阶段无需二次下载
        assertNotNull(repo.downloadedApk())
    }

    @Test
    fun `reports up to date and discards downloaded apk`() = runTest {
        val repo = repository(remote = ApkInfo(TEST_PACKAGE, versionCode = 5, versionName = "1.0.5"))

        val result = repo.checkForUpdate()

        assertTrue(result is UpdateCheckResult.UpToDate)
        assertNull(repo.downloadedApk())
    }

    @Test
    fun `older remote apk is treated as up to date`() = runTest {
        val repo = repository(remote = ApkInfo(TEST_PACKAGE, versionCode = 4, versionName = "1.0.4"))

        val result = repo.checkForUpdate()

        assertTrue(result is UpdateCheckResult.UpToDate)
        assertNull(repo.downloadedApk())
    }

    @Test
    fun `reports failure when download throws`() = runTest {
        val repo = repository(downloader = FileDownloader { _, _, _ -> throw IOException("boom") })

        val result = repo.checkForUpdate()

        assertTrue(result is UpdateCheckResult.Failed)
        assertTrue((result as UpdateCheckResult.Failed).message.contains("下载更新包失败"))
        assertNull(repo.downloadedApk())
    }

    @Test
    fun `reports failure when apk cannot be parsed`() = runTest {
        val repo = repository(remote = null)

        val result = repo.checkForUpdate()

        assertTrue(result is UpdateCheckResult.Failed)
        assertNull(repo.downloadedApk())
    }

    @Test
    fun `reports failure when apk package name mismatches`() = runTest {
        val repo = repository(remote = ApkInfo("com.other.app", versionCode = 6, versionName = "1.0.6"))

        val result = repo.checkForUpdate()

        assertTrue(result is UpdateCheckResult.Failed)
        assertTrue((result as UpdateCheckResult.Failed).message.contains("不匹配"))
        assertNull(repo.downloadedApk())
    }

    @Test
    fun `forwards download progress`() = runTest {
        val progress = mutableListOf<Float>()
        val repo = repository()

        repo.checkForUpdate { progress.add(it) }

        assertEquals(listOf(0.5f, 1f), progress)
    }

    @Test
    fun `clears stale apk before downloading`() = runTest {
        apkFile.parentFile?.mkdirs()
        apkFile.writeText("stale")
        val repo = repository(remote = ApkInfo(TEST_PACKAGE, versionCode = 4, versionName = "1.0.4"))

        repo.checkForUpdate()

        assertNull(repo.downloadedApk())
    }

    @Test
    fun `clearDownloadedApk removes file`() = runTest {
        val repo = repository()
        repo.checkForUpdate()

        repo.clearDownloadedApk()

        assertNull(repo.downloadedApk())
    }
}
