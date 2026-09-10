package com.qianrenni.reading.viewmodels.app

import com.qianrenni.reading.data.repository.AppUpdateRepository
import com.qianrenni.reading.data.repository.AppVersion
import com.qianrenni.reading.data.repository.UpdateCheckResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val installed = AppVersion(versionCode = 5, versionName = "1.0.5")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeRepository(
        result: UpdateCheckResult,
        apk: File? = null,
        gate: CompletableDeferred<Unit>? = null,
    ) = object : AppUpdateRepository {
        override fun currentVersion() = installed
        override fun apkUrl() = "http://example.com/static/guga.apk"

        override suspend fun checkForUpdate(onProgress: (Float) -> Unit): UpdateCheckResult {
            onProgress(0.5f)
            gate?.await()
            return result
        }

        override fun downloadedApk(): File? = apk
        override fun clearDownloadedApk() = Unit
    }

    @Test
    fun `exposes installed version on start`() = runTest(testDispatcher) {
        val vm = AppUpdateViewModel(
            fakeRepository(UpdateCheckResult.UpToDate(installed)),
            testDispatcher
        )

        advanceUntilIdle()

        assertEquals("1.0.5", vm.state.value.currentVersionName)
        assertTrue(vm.state.value.status is UpdateStatus.Idle)
    }

    @Test
    fun `maps up to date result`() = runTest(testDispatcher) {
        val vm = AppUpdateViewModel(
            fakeRepository(UpdateCheckResult.UpToDate(installed)),
            testDispatcher
        )

        vm.checkForUpdate()
        advanceUntilIdle()

        assertEquals(UpdateStatus.UpToDate("1.0.5"), vm.state.value.status)
    }

    @Test
    fun `maps available result with downloaded apk`() = runTest(testDispatcher) {
        val apk = File("guga.apk")
        val remote = AppVersion(versionCode = 6, versionName = "1.0.6")
        val vm = AppUpdateViewModel(
            fakeRepository(UpdateCheckResult.UpdateAvailable(installed, remote), apk = apk),
            testDispatcher
        )

        vm.checkForUpdate()
        advanceUntilIdle()

        assertEquals(UpdateStatus.Available("1.0.6", apk), vm.state.value.status)
    }

    @Test
    fun `reports failure when downloaded apk disappeared`() = runTest(testDispatcher) {
        val remote = AppVersion(versionCode = 6, versionName = "1.0.6")
        val vm = AppUpdateViewModel(
            fakeRepository(UpdateCheckResult.UpdateAvailable(installed, remote), apk = null),
            testDispatcher
        )

        vm.checkForUpdate()
        advanceUntilIdle()

        assertTrue(vm.state.value.status is UpdateStatus.Failed)
    }

    @Test
    fun `maps failed result`() = runTest(testDispatcher) {
        val vm = AppUpdateViewModel(
            fakeRepository(UpdateCheckResult.Failed("下载更新包失败：boom")),
            testDispatcher
        )

        vm.checkForUpdate()
        advanceUntilIdle()

        assertEquals(UpdateStatus.Failed("下载更新包失败：boom"), vm.state.value.status)
    }

    @Test
    fun `dismiss resets status to idle`() = runTest(testDispatcher) {
        val vm = AppUpdateViewModel(
            fakeRepository(UpdateCheckResult.UpToDate(installed)),
            testDispatcher
        )
        vm.checkForUpdate()
        advanceUntilIdle()

        vm.dismiss()

        assertTrue(vm.state.value.status is UpdateStatus.Idle)
    }

    @Test
    fun `dismiss is ignored while busy`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        val vm = AppUpdateViewModel(
            fakeRepository(UpdateCheckResult.UpToDate(installed), gate = gate),
            testDispatcher
        )

        vm.checkForUpdate()
        advanceUntilIdle()

        assertTrue(vm.state.value.isBusy)
        vm.dismiss()
        assertTrue(vm.state.value.isBusy)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(vm.state.value.isBusy)
        assertTrue(vm.state.value.status is UpdateStatus.UpToDate)
    }
}
