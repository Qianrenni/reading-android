package com.qianrenni.reading.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavBackStack
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NavigationState
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.navigation.PrivacyPolicy
import com.qianrenni.reading.views.legal.PrivacyPolicyView
import org.junit.Rule
import org.junit.Test

/**
 * 隐私政策页面的 Compose UI 测试（纯内容渲染，无需网络/依赖注入）。
 */
class PrivacyPolicyViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun navigator(): Navigator {
        val state = NavigationState(
            startRoute = Home,
            topLevelRoute = mutableStateOf(Home),
            backStacks = mapOf(Home to NavBackStack(Home, PrivacyPolicy))
        )
        return Navigator(state)
    }

    @Test
    fun rendersTitleAndBackButton() {
        composeRule.setContent { MaterialTheme { PrivacyPolicyView(navigator()) } }

        composeRule.onNodeWithText("用户协议与隐私政策").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").assertIsDisplayed()
    }

    @Test
    fun rendersPolicySections() {
        composeRule.setContent { MaterialTheme { PrivacyPolicyView(navigator()) } }

        composeRule.onNodeWithText("一、引言与同意").assertExists()
        composeRule.onNodeWithText("三、账号注册与用户行为规范").assertExists()
        composeRule.onNodeWithText("六、信息的使用目的").assertExists()
        composeRule.onNodeWithText("十四、生效日期").assertExists()
    }
}
