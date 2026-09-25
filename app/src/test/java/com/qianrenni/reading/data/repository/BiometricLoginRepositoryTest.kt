package com.qianrenni.reading.data.repository

import com.qianrenni.reading.FakeDeviceLoginApi
import com.qianrenni.reading.InMemoryKeyValueStore
import com.qianrenni.reading.data.model.DeviceCredential
import com.qianrenni.reading.data.model.DeviceLoginResponse
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.testUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 指纹解锁仓库测试：设备凭据的保存、使用（轮换）、失效清理与撤销。 */
class BiometricLoginRepositoryTest {

    private val store = InMemoryKeyValueStore()
    private val api = FakeDeviceLoginApi()

    private fun repository() = BiometricLoginRepositoryImpl(store, api)

    private fun deviceLoginResponse(
        deviceToken: String = "device-2",
        user: String = "tom"
    ) = DeviceLoginResponse(
        deviceToken = deviceToken,
        expiresIn = 1000,
        accessToken = "access",
        refreshToken = "refresh",
        tokenType = "Bearer",
        user = testUser(name = user)
    )

    @Test
    fun `默认未开启`() {
        val repository = repository()
        assertFalse(repository.isEnabled.value)
        assertFalse(repository.hasCredential())
    }

    @Test
    fun `enable 成功后保存凭据并置为已开启`() = runTest {
        api.createResult = NetworkResult.Success(DeviceCredential("device-1", 1000))
        val repository = repository()

        assertTrue(repository.enable())

        assertTrue(repository.isEnabled.value)
        assertEquals("device-1", store.getString("device_token"))
    }

    @Test
    fun `enable 服务端失败时不开启`() = runTest {
        api.createResult = NetworkResult.Failure("失败")
        val repository = repository()

        assertFalse(repository.enable())

        assertFalse(repository.isEnabled.value)
        assertNull(store.getString("device_token"))
    }

    @Test
    fun `已保存凭据时初始即为已开启`() {
        store.putString("device_token", "device-0")
        assertTrue(repository().isEnabled.value)
    }

    @Test
    fun `login 用凭据换令牌并覆盖轮换后的新凭据`() = runTest {
        store.putString("device_token", "device-1")
        api.loginResult = NetworkResult.Success(deviceLoginResponse(deviceToken = "device-2"))
        val repository = repository()

        val response = repository.login()

        assertEquals("device-1", api.lastLoginToken)
        assertEquals("access", response?.accessToken)
        assertEquals("refresh", response?.refreshToken)
        assertEquals("tom", response?.user?.userName)
        // 服务端轮换后的凭据必须落盘，否则下次登录会拿着已作废的旧凭据
        assertEquals("device-2", store.getString("device_token"))
        assertTrue(repository.isEnabled.value)
    }

    @Test
    fun `login 无凭据时返回 null 且不请求`() = runTest {
        val repository = repository()

        assertNull(repository.login())
        assertNull(api.lastLoginToken)
    }

    @Test
    fun `login 被服务端拒绝时清除凭据`() = runTest {
        store.putString("device_token", "device-1")
        api.loginResult = NetworkResult.Failure("指纹登录已失效，请使用账号密码重新登录", code = 1)
        val repository = repository()

        assertNull(repository.login())

        assertFalse(repository.isEnabled.value)
        assertFalse(repository.hasCredential())
    }

    @Test
    fun `login 网络异常时保留凭据以便重试`() = runTest {
        store.putString("device_token", "device-1")
        api.loginResult = NetworkResult.Failure("网络错误:服务器连接异常")
        val repository = repository()

        assertNull(repository.login())

        assertTrue(repository.isEnabled.value)
        assertEquals("device-1", store.getString("device_token"))
    }

    @Test
    fun `disable 清除本地凭据并撤销服务端凭据`() = runTest {
        store.putString("device_token", "device-1")
        val repository = repository()

        repository.disable()

        assertFalse(repository.isEnabled.value)
        assertNull(store.getString("device_token"))
        assertEquals(1, api.revokeCount)
    }

    @Test
    fun `disable 撤销失败也仍然关闭本地开关`() = runTest {
        store.putString("device_token", "device-1")
        api.revokeResult = NetworkResult.Failure("网络错误")
        val repository = repository()

        repository.disable()

        assertFalse(repository.isEnabled.value)
        assertNull(store.getString("device_token"))
    }

    @Test
    fun `disable 在无凭据时不触发撤销请求`() = runTest {
        val repository = repository()

        repository.disable()

        assertEquals(0, api.revokeCount)
        assertFalse(repository.isEnabled.value)
    }
}
