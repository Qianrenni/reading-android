package com.qianrenni.reading.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import com.qianrenni.reading.FakeAuthApi
import com.qianrenni.reading.FakeAuthRepository
import com.qianrenni.reading.data.model.LoginResponse
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.testUser
import com.qianrenni.reading.viewmodels.auth.LoginViewModel
import com.qianrenni.reading.views.auth.LoginView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 登录页面的 Compose UI 测试。
 * 通过注入 Fake LoginViewModel 隔离网络；重点覆盖隐私协议同意门禁。
 */
class LoginViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun navigator(): Navigator {
        val state = NavigationState(
            startRoute = Home,
            topLevelRoute = mutableStateOf(Home),
            backStacks = mapOf(Home to NavBackStack(Home))
        )
        return Navigator(state)
    }

    @Test
    fun rendersLoginFormFields() {
        val vm = LoginViewModel(FakeAuthApi(), FakeAuthRepository())
        composeRule.setContent { MaterialTheme { LoginView(navigator(), vm) } }

        composeRule.onNodeWithText("用户登录").assertIsDisplayed()
        composeRule.onNodeWithText("邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("密码").assertIsDisplayed()
        composeRule.onNodeWithText("我已阅读并同意").assertIsDisplayed()
    }

    @Test
    fun loginButtonDisabledUntilConsent() {
        val vm = LoginViewModel(FakeAuthApi(), FakeAuthRepository())
        composeRule.setContent { MaterialTheme { LoginView(navigator(), vm) } }

        // 未勾选协议前登录按钮禁用
        composeRule.onNodeWithTag("login_button").assertIsNotEnabled()

        // 勾选同意后可用
        composeRule.onNodeWithTag("consent_checkbox").performClick()
        composeRule.onNodeWithTag("login_button").assertIsEnabled()
    }

    @Test
    fun loginWithConsentCallsViewModelAndSubmitsCredentials() {
        val api = FakeAuthApi().apply {
            loginResult = NetworkResult.Success(
                LoginResponse("access", "refresh", "Bearer", testUser())
            )
        }
        val vm = LoginViewModel(api, FakeAuthRepository())
        composeRule.setContent { MaterialTheme { LoginView(navigator(), vm) } }

        composeRule.onNodeWithTag("consent_checkbox").performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("tom@e.c")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("secret")
        composeRule.onAllNodes(hasSetTextAction())[2].performTextInput("cap")
        composeRule.onNodeWithTag("login_button").performClick()

        // 登录在 viewModelScope 中异步执行，等待其完成
        composeRule.waitUntil(timeoutMillis = 5_000) { api.loginCalled }
        assertTrue(api.loginCalled)
        assertEquals("tom@e.c", api.lastLoginRequest?.username)
        assertEquals("secret", api.lastLoginRequest?.password)
        assertEquals("cap", api.lastLoginRequest?.captcha)
    }
}
