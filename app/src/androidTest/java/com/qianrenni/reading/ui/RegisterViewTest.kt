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
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.viewmodels.auth.RegisterViewModel
import com.qianrenni.reading.views.auth.RegisterView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 注册页面的 Compose UI 测试。
 * 通过注入 Fake AuthApi 隔离网络；覆盖表单渲染、协议同意门禁、注册与邮箱验证。
 */
class RegisterViewTest {

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

    private fun setContent(api: FakeAuthApi = FakeAuthApi()) {
        val vm = RegisterViewModel(api)
        composeRule.setContent { MaterialTheme { RegisterView(navigator(), vm) } }
    }

    @Test
    fun rendersRegisterFormFields() {
        setContent()

        composeRule.onNodeWithText("新用户注册").assertIsDisplayed()
        composeRule.onNodeWithText("用户名").assertIsDisplayed()
        composeRule.onNodeWithText("密码").assertIsDisplayed()
        composeRule.onNodeWithText("确认密码").assertIsDisplayed()
        composeRule.onNodeWithText("邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("验证码").assertIsDisplayed()
        composeRule.onNodeWithText("我已阅读并同意").assertIsDisplayed()
        composeRule.onNodeWithText("验证邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("注册").assertIsDisplayed()
    }

    @Test
    fun registerButtonDisabledUntilConsent() {
        setContent()

        composeRule.onNodeWithTag("register_button").assertIsNotEnabled()

        composeRule.onNodeWithTag("consent_checkbox").performClick()
        composeRule.onNodeWithTag("register_button").assertIsEnabled()
    }

    @Test
    fun registerWithConsentSubmitsCredentials() {
        val api = FakeAuthApi()
        setContent(api)

        composeRule.onNodeWithTag("consent_checkbox").performClick()
        // 5 个文本输入框：用户名/密码/确认密码/邮箱/验证码
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("tom")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("secret")
        composeRule.onAllNodes(hasSetTextAction())[2].performTextInput("secret")
        composeRule.onAllNodes(hasSetTextAction())[3].performTextInput("tom@e.c")
        composeRule.onAllNodes(hasSetTextAction())[4].performTextInput("cap")
        composeRule.onNodeWithTag("register_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { api.registerCalled }
        assertTrue(api.registerCalled)
        val request = api.lastRegisterRequest
        assertEquals("tom", request?.user?.userName)
        assertEquals("secret", request?.user?.password)
        assertEquals("tom@e.c", request?.user?.email)
        assertEquals("cap", request?.captcha)
    }

    @Test
    fun verifyEmailWithValidEmailCallsApi() {
        val api = FakeAuthApi()
        setContent(api)

        composeRule.onAllNodes(hasSetTextAction())[3].performTextInput("tom@e.c")
        composeRule.onNodeWithText("验证邮箱").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { api.verifyEmailCalled }
        assertTrue(api.verifyEmailCalled)
        assertEquals("tom@e.c", api.lastVerifyEmailRequest?.email)
    }
}
