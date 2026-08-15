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

/**
 * 正式版《用户协议与隐私政策》。
 * 提示：上线前请将【运营主体名称】【联系方式】等占位内容替换为贵司真实信息，并由法务审核后使用。
 */
private val policySections = listOf(
    PolicySection(
        title = "一、引言与同意",
        body = "欢迎使用本阅读应用（以下简称“本应用”）。本应用由【运营主体名称】（以下简称“我们”）开发并运营。" +
            "我们深知个人信息对您的重要性，并会尽全力保护您的个人信息安全。我们致力于维持您对我们的信任，恪守以下原则：" +
            "权责一致原则、目的明确原则、选择同意原则、最少够用原则、确保安全原则、主体参与原则、公开透明原则。" +
            "请您在使用本应用前，仔细阅读并充分理解本《用户协议与隐私政策》（以下简称“本政策”）的全部内容。" +
            "您勾选“我已阅读并同意”、点击同意或开始使用本应用，即表示您已充分理解并同意本政策；如您不同意本政策中的任何内容，请立即停止使用本应用。"
    ),
    PolicySection(
        title = "二、定义",
        body = "1. 个人信息：指以电子或者其他方式记录的与已识别或者可识别的自然人有关的各种信息，不包括匿名化处理后的信息。\n" +
            "2. 敏感个人信息：指一旦泄露或者非法使用，容易导致自然人的人格尊严受到侵害或者人身、财产安全受到危害的个人信息，包括生物识别、宗教信仰、特定身份、医疗健康、金融账户、行踪轨迹等信息，以及不满十四周岁未成年人的个人信息。\n" +
            "3. 匿名化：指个人信息经过处理无法识别特定自然人且不能复原的过程。\n" +
            "4. 本政策中未定义的概念，适用法律法规的规定。"
    ),
    PolicySection(
        title = "三、账号注册与用户行为规范",
        body = "1. 您需注册账号并使用验证码登录。注册时应提供真实、准确、完整的邮箱、用户名等信息；信息发生变更时应及时更新。\n" +
            "2. 您应妥善保管账号、密码及验证码，不得出借、转让或出租账号；因您保管不善或主动泄露导致的损失由您自行承担。\n" +
            "3. 您承诺遵守法律法规，不得利用本应用从事下列行为：发布违法或不良信息、侵犯他人知识产权或隐私、实施网络攻击、从事任何形式的欺诈或恶意刷量、干扰本应用正常运行等。\n" +
            "4. 若您违反本协议，我们有权视情节采取警示、限制功能、暂停或终止服务等措施，并保留追究法律责任的权利。\n" +
            "5. 本应用提供的书籍内容版权归权利人所有，您仅可出于个人学习、欣赏目的阅读，不得进行复制、传播、售卖等商业利用。"
    ),
    PolicySection(
        title = "四、我们收集的信息",
        body = "在您使用本应用的过程中，为实现产品功能，我们可能收集以下类别信息：\n" +
            "1. 账号信息：用户名、邮箱、头像等您在注册及完善资料时主动提供的信息；\n" +
            "2. 内容信息：您的书架、收藏、阅读记录、阅读进度、浏览历史，以及您发布的评论、反馈等内容；\n" +
            "3. 设备与日志信息：设备型号、操作系统及版本、屏幕分辨率、网络类型、IP 地址、应用版本、访问时间与请求日志等，用于保障服务安全与稳定性；\n" +
            "4. 您主动提交的其他信息：如通过客服或意见反馈功能提交的问题与联系方式。\n" +
            "我们仅在本政策所述目的所需的范围内收集最少必要的信息，不会收集与提供服务无关的个人信息。"
    ),
    PolicySection(
        title = "五、Cookie 及同类技术",
        body = "本应用可能使用 Cookie、本地存储（LocalStorage）等同类技术以保存您的登录状态与使用偏好，从而提供更流畅的体验。" +
            "您可通过系统设置或应用内设置拒绝或清除相关数据，但可能影响部分功能（如自动登录）的正常使用。"
    ),
    PolicySection(
        title = "六、信息的使用目的",
        body = "我们收集的信息将用于以下目的：\n" +
            "1. 提供、维护和改进本应用的产品与服务；\n" +
            "2. 同步您的阅读进度、书架等数据，保障多设备登录时的一致体验；\n" +
            "3. 账号登录、身份验证与安全保障；\n" +
            "4. 响应您的评论、反馈与客服请求；\n" +
            "5. 在您授权同意的前提下进行个性化推荐或营销活动；\n" +
            "6. 在法律允许范围内开展统计分析、产品优化与安全风控。\n" +
            "未经您的单独同意，我们不会将您的个人信息用于本政策未载明的其他用途。"
    ),
    PolicySection(
        title = "七、信息的共享、转让与公开披露",
        body = "我们不会向任何第三方出售您的个人信息。仅在以下情形下，我们可能共享、转让或公开披露您的个人信息：\n" +
            "1. 获得您的明确同意或主动授权；\n" +
            "2. 根据法律法规、司法程序或行政监管的强制性要求；\n" +
            "3. 为维护国家安全、社会公共利益所必需；\n" +
            "4. 为维护本应用的安全稳定运行，或保护您或其他用户、权利人的生命、财产等合法权益所合理必需；\n" +
            "5. 与关联公司或必要的服务提供商（如云服务商、短信服务商）共享，且仅在实现本政策所述目的所必需的范围内，并要求其遵守保密义务。"
    ),
    PolicySection(
        title = "八、第三方服务",
        body = "本应用可能接入第三方提供的服务或 SDK（例如：网络请求框架、图片加载库、统计服务等），以实现对应功能。" +
            "第三方 SDK 仅在实现功能所必需的范围内收集信息，并遵循其自身隐私政策。我们会在接入前对第三方 SDK 进行安全评估，并要求其遵守法律法规与行业规范。" +
            "您在使用第三方服务时，请关注并阅读该第三方的隐私政策。"
    ),
    PolicySection(
        title = "九、信息的存储与保护",
        body = "1. 存储地点：您的个人信息将存储于中华人民共和国境内。如需向境外传输，我们将依法履行相应的告知与同意程序。\n" +
            "2. 存储期限：我们仅在实现本政策所述目的所必需的期限内保留您的个人信息，法律法规另有规定的除外。您注销账号后，我们将按照法律法规要求删除或匿名化处理您的个人信息。\n" +
            "3. 安全措施：我们采用加密传输（HTTPS）、令牌加密存储（EncryptedSharedPreferences）、访问控制、安全审计等业内通行的技术与管理措施保护您的信息安全，并定期进行安全评估。\n" +
            "4. 尽管我们已采取合理措施，但任何网络传输都无法保证绝对安全。请您妥善保管账号密码，谨慎在公共网络环境下使用本应用；如发现账号被盗用，请立即联系我们。"
    ),
    PolicySection(
        title = "十、未成年人保护",
        body = "我们非常重视未成年人个人信息保护。若您是未满 18 周岁的未成年人，请在监护人陪同并征得监护人明确同意后使用本应用。" +
            "对于不满 14 周岁的儿童，我们仅在监护人明确同意且出于保护儿童目的所必需时处理其个人信息。" +
            "若我们发现在未事先获得监护人同意的情况下收集了儿童个人信息，将设法尽快删除相关数据。"
    ),
    PolicySection(
        title = "十一、您的权利",
        body = "依据法律法规，您享有以下权利，可通过【个人中心】或本政策第十三条的联系方式行使：\n" +
            "1. 查询权：查询您的账号信息与使用记录；\n" +
            "2. 更正权：更正不准确或不完整的个人信息；\n" +
            "3. 删除权：删除您的评论、书架记录等个人内容，或在符合条件时申请删除个人信息；\n" +
            "4. 注销权：申请注销账号，我们将按照法律法规处理您的个人信息；\n" +
            "5. 撤回同意权：撤回您对本政策相关处理的授权同意（撤回不影响撤回前基于同意已进行的处理）。\n" +
            "如您对个人信息处理有异议或投诉，您可以随时与我们联系，我们将在 15 个工作日内予以答复。"
    ),
    PolicySection(
        title = "十二、本政策的更新",
        body = "我们可能根据法律法规变化、业务功能调整等对本政策进行更新。更新后的政策将在本应用内以显著方式公布。" +
            "对于重大变更（如处理个人信息的目的、方式、范围发生重大变化，或您享有的权利发生重大变化等），我们将以弹窗、页面提示等方式另行通知您。" +
            "本政策更新后，若您继续使用本应用，即视为您已阅读并同意更新后的内容。"
    ),
    PolicySection(
        title = "十三、联系我们",
        body = "如您对本政策或个人信息保护有任何疑问、意见或建议，请通过以下方式与我们联系：\n" +
            "1. 应用内：【个人中心 → 意见反馈】；\n" +
            "2. 电子邮箱：【privacy@example.com】；\n" +
            "3. 邮寄地址：【运营主体联系地址】。\n" +
            "我们将在收到您的反馈后 15 个工作日内予以回复。若您对我们的处理结果不满意，还可以向网信、电信、公安、市场监管等监管部门进行投诉或举报。"
    ),
    PolicySection(
        title = "十四、生效日期",
        body = "本政策自【2026 年 8 月 16 日】起生效。"
    )
)
