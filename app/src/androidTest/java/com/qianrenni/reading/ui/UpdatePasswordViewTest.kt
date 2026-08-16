package com.qianrenni.reading.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import com.qianrenni.reading.FakeAuthRepository
import com.qianrenni.reading.FakeUserApi
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.testUser
import com.qianrenni.reading.viewmodels.auth.UpdatePasswordViewModel
import com.qianrenni.reading.views.auth.UpdatePasswordView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 修改密码页面的 Compose UI 测试。
 * 通过注入 Fake UserApi + Fake AuthRepository 隔离网络与登录态；覆盖表单渲染与成功提交。
 */
class UpdatePasswordViewTest {

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
    fun rendersUpdatePasswordForm() {
        val vm = UpdatePasswordViewModel(FakeUserApi(), FakeAuthRepository())
        composeRule.setContent { MaterialTheme { UpdatePasswordView(navigator(), vm) } }

        composeRule.onNodeWithText("邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("旧密码").assertIsDisplayed()
        composeRule.onNodeWithText("新密码").assertIsDisplayed()
        composeRule.onNodeWithText("确认密码").assertIsDisplayed()
        composeRule.onNodeWithText("返回").assertIsDisplayed()
        // 标题与按钮均为“修改密码”，共两个节点
        composeRule.onAllNodes(hasText("修改密码")).assertCountEquals(2)
    }

    @Test
    fun updatePasswordSuccessCallsApiAndClearsAuth() {
        val api = FakeUserApi()
        val authRepository = FakeAuthRepository(initialUser = testUser())
        val vm = UpdatePasswordViewModel(api, authRepository)
        composeRule.setContent { MaterialTheme { UpdatePasswordView(navigator(), vm) } }

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("tom@e.c")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("oldPass")
        composeRule.onAllNodes(hasSetTextAction())[2].performTextInput("newPass")
        composeRule.onAllNodes(hasSetTextAction())[3].performTextInput("newPass")
        // 标题与按钮同名，点击最后一个（按钮）
        composeRule.onAllNodes(hasText("修改密码"))[1].performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { api.updatePasswordCalled }
        assertTrue(api.updatePasswordCalled)
        val request = api.lastUpdatePasswordRequest
        assertEquals("tom@e.c", request?.userName)
        assertEquals("oldPass", request?.oldPassword)
        assertEquals("newPass", request?.newPassword)
        // 成功后应清除本地登录态（异步执行，轮询等待）
        composeRule.waitUntil(timeoutMillis = 5_000) { authRepository.user.value == null }
    }
}
