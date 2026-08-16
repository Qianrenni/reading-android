package com.qianrenni.reading.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import com.qianrenni.reading.FakeAuthRepository
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.Login
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.testUser
import com.qianrenni.reading.viewmodels.auth.AuthViewModel
import com.qianrenni.reading.views.user.ProfileView
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 个人中心页面的 Compose UI 测试。
 * 通过注入 Fake AuthRepository 隔离登录态；覆盖用户信息渲染、服务器设置对话框与退出登录。
 */
class ProfileViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(authRepository: FakeAuthRepository = FakeAuthRepository()): Navigator {
        val state = NavigationState(
            startRoute = Home,
            topLevelRoute = mutableStateOf(Home),
            backStacks = mapOf(Home to NavBackStack(Home))
        )
        val navigator = Navigator(state)
        val authViewModel = AuthViewModel(authRepository)
        composeRule.setContent { MaterialTheme { ProfileView(navigator, authViewModel) } }
        return navigator
    }

    @Test
    fun rendersUserInfo() {
        setContent(FakeAuthRepository(initialUser = testUser(name = "tom")))

        composeRule.onNodeWithText("个人中心").assertIsDisplayed()
        composeRule.onNodeWithText("tom").assertIsDisplayed()
        composeRule.onNodeWithText("tom@e.c").assertIsDisplayed()
        composeRule.onNodeWithText("已激活").assertIsDisplayed()
        composeRule.onNodeWithText("服务器设置").assertIsDisplayed()
        composeRule.onNodeWithText("修改密码").assertIsDisplayed()
        composeRule.onNodeWithText("退出登录").assertIsDisplayed()
    }

    @Test
    fun serverDialogOpensAndCloses() {
        setContent(FakeAuthRepository(initialUser = testUser()))

        composeRule.onNodeWithText("服务器设置").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("服务器地址设置")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("服务器地址设置").assertIsDisplayed()
        composeRule.onNodeWithText("Base URL").assertIsDisplayed()

        composeRule.onNodeWithText("取消").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("服务器地址设置")).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun logoutClearsAuthAndNavigatesToLogin() {
        val authRepository = FakeAuthRepository(initialUser = testUser())
        val navigator = setContent(authRepository)

        composeRule.onNodeWithText("退出登录").performClick()

        assertNull(authRepository.user.value)
        assertTrue(navigator.currentState == Login)
    }
}
