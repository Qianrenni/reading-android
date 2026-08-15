package com.qianrenni.reading.viewmodels.auth

import com.qianrenni.reading.FakeAuthRepository
import com.qianrenni.reading.navigation.BookInfo
import com.qianrenni.reading.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isLogin reflects auth repository user`() = runTest(testDispatcher) {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)
        advanceUntilIdle()
        assertEquals(false, vm.isLogin.value)

        repo.setUser(testUser())
        advanceUntilIdle()
        assertEquals(true, vm.isLogin.value)

        repo.setUser(null)
        advanceUntilIdle()
        assertEquals(false, vm.isLogin.value)
    }

    @Test
    fun `redirect url is consumed once`() = runTest(testDispatcher) {
        val vm = AuthViewModel(FakeAuthRepository())
        vm.setRedirectUrl(BookInfo(5))
        assertEquals(BookInfo(5), vm.getRedirectUrl())
        assertNull(vm.getRedirectUrl())
    }

    @Test
    fun `getUser exposes repository user flow`() {
        val vm = AuthViewModel(FakeAuthRepository(testUser(id = 3)))
        assertEquals(3, vm.getUser().value?.id)
    }

    @Test
    fun `clear delegates to repository`() = runTest(testDispatcher) {
        val repo = FakeAuthRepository(testUser())
        val vm = AuthViewModel(repo)
        advanceUntilIdle()

        vm.clear()

        assertNull(repo.userFlow.value)
    }
}
