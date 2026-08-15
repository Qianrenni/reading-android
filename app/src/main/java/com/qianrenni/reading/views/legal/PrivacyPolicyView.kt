package com.qianrenni.reading.views.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qianrenni.reading.navigation.Navigator

/**
 * 用户协议与隐私政策页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyView(navigator: Navigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户协议与隐私政策") },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "更新日期：2026年8月16日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            policySections.forEach { section ->
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = section.body,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private data class PolicySection(val title: String, val body: String)

private val policySections = listOf(
    PolicySection(
        title = "一、引言",
        body = "欢迎使用本阅读应用（以下简称“本应用”）。本应用由【开发者】开发并运营。" +
            "我们深知个人信息对您的重要性，将按照法律法规要求，采取相应安全保护措施，尽力保护您的个人信息安全可控。" +
            "在使用本应用前，请您务必仔细阅读并充分理解本《用户协议与隐私政策》（以下简称“本政策”）。" +
            "您点击“同意”或开始使用本应用，即表示您已充分理解并同意本政策的全部内容。"
    ),
    PolicySection(
        title = "二、用户账号与协议",
        body = "1. 您需要注册账号并使用验证码登录，注册时应提供真实、准确、完整的邮箱、用户名等信息。\n" +
            "2. 您应妥善保管账号、密码及验证码，因您保管不善造成的损失由您自行承担。\n" +
            "3. 您承诺不利用本应用从事违反法律法规、侵害他人权益的行为。\n" +
            "4. 若您违反本协议，我们有权暂停或终止向您提供服务。"
    ),
    PolicySection(
        title = "三、我们收集的信息",
        body = "为了向您提供阅读、书架、评论等服务，我们可能收集以下信息：\n" +
            "1. 账号信息：用户名、邮箱、头像等注册信息；\n" +
            "2. 使用信息：您的阅读记录、书架、阅读进度、浏览记录等；\n" +
            "3. 设备信息：设备型号、操作系统版本、网络状态等（用于保障服务安全与稳定性）；\n" +
            "4. 您主动提交的评论、反馈等内容。"
    ),
    PolicySection(
        title = "四、信息的使用",
        body = "我们收集的信息将用于：\n" +
            "1. 提供、维护和改进我们的产品与服务；\n" +
            "2. 同步您的阅读进度与书架数据，保障多端体验；\n" +
            "3. 安全风控与异常排查；\n" +
            "4. 在法律允许的范围内开展数据分析与产品优化。"
    ),
    PolicySection(
        title = "五、信息的共享与披露",
        body = "我们不会向任何第三方出售您的个人信息。除以下情形外，我们不会共享、转让或公开披露您的个人信息：\n" +
            "1. 获得您的明确同意；\n" +
            "2. 根据法律法规、司法或行政机关的强制性要求；\n" +
            "3. 为维护本应用的安全稳定运行、保护您或其他用户的生命财产安全所必需。"
    ),
    PolicySection(
        title = "六、信息安全",
        body = "我们采取行业通行的加密技术（如 HTTPS 传输、令牌加密存储）和管理措施保护您的信息安全。" +
            "但请注意，任何网络传输方式都无法保证 100% 安全，请您妥善保管账号密码，并谨慎地在公共网络环境下使用本应用。"
    ),
    PolicySection(
        title = "七、您的权利",
        body = "您有权随时查看、更正您的个人信息，也可以要求注销账号或删除相关数据。您可以通过【个人中心-设置】或在意见反馈中联系我们行使上述权利。" +
            "您也可以随时停止使用本应用并退出登录。"
    ),
    PolicySection(
        title = "八、未成年人保护",
        body = "我们非常重视未成年人个人信息保护。若您是未满 18 周岁的未成年人，请在监护人陪同下阅读本政策，并在征得监护人同意后使用本应用。"
    ),
    PolicySection(
        title = "九、政策的更新",
        body = "我们可能适时对本政策进行更新。更新后的政策将在本应用内公布，重大变更将以显著方式提示您。政策更新后，您继续使用本应用即视为同意更新后的内容。"
    ),
    PolicySection(
        title = "十、联系我们",
        body = "如您对本政策或个人信息保护有任何疑问、意见或建议，请通过应用内的联系方式与我们取得联系，我们将在 15 个工作日内回复。"
    )
)
