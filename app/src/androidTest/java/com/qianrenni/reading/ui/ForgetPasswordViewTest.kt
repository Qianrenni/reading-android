package com.qianrenni.reading.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import com.qianrenni.reading.FakeUserApi
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.viewmodels.auth.ForgetPasswordViewModel
import com.qianrenni.reading.views.auth.ForgetPasswordView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 忘记密码页面的 Compose UI 测试。
 * 通过注入 Fake UserApi 隔离网络；覆盖表单渲染、发送验证码与重置密码。
 */
class ForgetPasswordViewTest {

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

    private fun setContent(api: FakeUserApi = FakeUserApi()) {
        val vm = ForgetPasswordViewModel(api)
        composeRule.setContent { MaterialTheme { ForgetPasswordView(navigator(), vm) } }
    }

    @Test
    fun rendersForgetPasswordForm() {
        setContent()

        composeRule.onNodeWithText("忘记密码").assertIsDisplayed()
        composeRule.onNodeWithText("邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("验证码").assertIsDisplayed()
        composeRule.onNodeWithText("新密码").assertIsDisplayed()
        composeRule.onNodeWithText("确认密码").assertIsDisplayed()
        composeRule.onNodeWithText("验证邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("重置密码").assertIsDisplayed()
        composeRule.onNodeWithText("返回登录").assertIsDisplayed()
    }

    @Test
    fun sendVerificationCodeCallsApi() {
        val api = FakeUserApi()
        setContent(api)

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("tom@e.c")
        composeRule.onNodeWithText("验证邮箱").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { api.sendCodeCalled }
        assertTrue(api.sendCodeCalled)
        assertEquals("tom@e.c", api.lastSendEmail)
    }

    @Test
    fun resetPasswordSuccessCallsApi() {
        val api = FakeUserApi()
        setContent(api)

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("tom@e.c")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("123456")
        composeRule.onAllNodes(hasSetTextAction())[2].performTextInput("newPass")
        composeRule.onAllNodes(hasSetTextAction())[3].performTextInput("newPass")
        composeRule.onNodeWithText("重置密码").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { api.resetPasswordCalled }
        assertTrue(api.resetPasswordCalled)
        val request = api.lastResetRequest
        assertEquals("tom@e.c", request?.userAccount)
        assertEquals("123456", request?.verifyCode)
        assertEquals("newPass", request?.password)
    }
}
