package com.qianrenni.reading.views.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.qianrenni.reading.components.SettingItem
import com.qianrenni.reading.data.model.User
import com.qianrenni.reading.data.repository.ThemeMode
import com.qianrenni.reading.data.repository.ThemeRepository
import com.qianrenni.reading.di.appContainer
import com.qianrenni.reading.navigation.Login
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.navigation.UpdatePassword
import com.qianrenni.reading.navigation.openWebPage
import com.qianrenni.reading.util.ApkInstaller
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.viewmodels.app.AppUpdateState
import com.qianrenni.reading.viewmodels.app.AppUpdateViewModel
import com.qianrenni.reading.viewmodels.app.UpdateStatus
import com.qianrenni.reading.viewmodels.auth.AuthViewModel
import kotlinx.coroutines.launch
import java.io.File

/** 后端默认头像。 */
private const val DEFAULT_AVATAR = "http://49.235.107.221:8000/static/guga.webp"

/** 「计算机知识」入口：在应用内 H5 容器打开的地址与标题。 */
private const val COMPUTER_KNOWLEDGE_URL = "https://qyani.netlify.app/#/"
private const val COMPUTER_KNOWLEDGE_TITLE = "星阑小筑·学习札记"

@Composable
fun ProfileView(
    navigator: Navigator,
    authViewModel: AuthViewModel = viewModel(factory = appContainer().viewModelFactory),
    themeRepository: ThemeRepository = appContainer().themeRepository,
    updateViewModel: AppUpdateViewModel = viewModel(factory = appContainer().viewModelFactory)
) {
    val user by authViewModel.getUser().collectAsStateWithLifecycle()
    val themeMode by themeRepository.mode.collectAsStateWithLifecycle()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appConfig = appContainer().appConfig
    var showServerDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(appConfig.currentBaseUrl()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "个人中心",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )

        // 顶部：个人信息（左侧头像 + 右侧用户名/账号信息）
        ProfileHeader(user = user)

        // 中部：设置列表（占满剩余高度，可滚动）
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column {
                    SettingItem(
                        title = "计算机知识",
                        onClick = {
                            navigator.openWebPage(
                                url = COMPUTER_KNOWLEDGE_URL,
                                title = COMPUTER_KNOWLEDGE_TITLE
                            )
                        }
                    )
                    SettingItem(
                        title = "WebVue组件库",
                        onClick = {
                            navigator.openWebPage(
                                url = "https://qyani-ui.netlify.app/#/",
                                title = "WebVue组件库"
                            )
                        }
                    )
                    SettingDivider()
                    SettingItem(
                        title = "主题外观",
                        onClick = { showThemeDialog = true }
                    )
                    SettingDivider()
                    SettingItem(
                        title = "服务器设置",
                        onClick = {
                            serverUrl = appConfig.currentBaseUrl()
                            showServerDialog = true
                        }
                    )
                    SettingDivider()
                    SettingItem(
                        title = "检查更新",
                        value = "v${updateState.currentVersionName.ifEmpty { "-" }}",
                        enabled = !updateState.isBusy,
                        onClick = { updateViewModel.checkForUpdate() }
                    )
                    SettingDivider()
                    SettingItem(
                        title = "修改密码",
                        onClick = { navigator.navigate(UpdatePassword) }
                    )
                }
            }
        }

        // 底部：退出登录
        OutlinedButton(
            onClick = {
                authViewModel.clear()
                navigator.navigate(Login)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("退出登录")
        }
    }

    // 主题外观选择对话框
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("主题外观") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = mode == themeMode,
                                    role = Role.RadioButton,
                                    onClick = {
                                        themeRepository.setMode(mode)
                                        showThemeDialog = false
                                    }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = mode == themeMode, onClick = null)
                            Text(text = mode.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
            }
        )
    }

    // 服务器地址设置对话框
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("服务器地址设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "修改后立即生效，无需重启应用。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        singleLine = true,
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    appConfig.setBaseUrl(serverUrl)
                    showServerDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 应用更新弹窗
    if (updateState.status !is UpdateStatus.Idle) {
        UpdateDialog(
            state = updateState,
            onDismiss = { updateViewModel.dismiss() },
            onInstall = { apk ->
                if (ApkInstaller.install(context, apk)) {
                    updateViewModel.dismiss()
                } else {
                    scope.launch { SnackBarManager.showMessage("请先允许本应用安装未知来源应用") }
                }
            }
        )
    }
}

/** 应用更新弹窗：根据 [AppUpdateState] 展示检查、下载与结果。 */
@Composable
private fun UpdateDialog(
    state: AppUpdateState,
    onDismiss: () -> Unit,
    onInstall: (File) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("应用更新") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "当前版本 v${state.currentVersionName.ifEmpty { "-" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when (val status = state.status) {
                    is UpdateStatus.Checking -> {
                        Text(status.message)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    is UpdateStatus.Downloading -> {
                        Text("正在下载新版本…")
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(status.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateStatus.UpToDate -> Text("已是最新版本 v${status.versionName}")

                    is UpdateStatus.Available -> Text("发现新版本 v${status.versionName}，点击「立即安装」完成升级")

                    is UpdateStatus.Failed -> Text(
                        text = status.message,
                        color = MaterialTheme.colorScheme.error
                    )

                    UpdateStatus.Idle -> Unit
                }
            }
        },
        confirmButton = {
            val status = state.status
            if (status is UpdateStatus.Available) {
                TextButton(onClick = { onInstall(status.installedApk) }) {
                    Text("立即安装")
                }
            } else {
                TextButton(onClick = onDismiss, enabled = !state.isBusy) {
                    Text("知道了")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isBusy) {
                Text(if (state.status is UpdateStatus.Available) "稍后再说" else "关闭")
            }
        }
    )
}

/** 顶部个人信息：左侧头像，右侧用户名与账号信息。 */
@Composable
private fun ProfileHeader(user: User?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (user?.avatar.isNullOrEmpty()) DEFAULT_AVATAR else user?.avatar,
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = user?.userName ?: "未知用户",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user?.email.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (user?.isActive == true) "已激活" else "未激活",
                style = MaterialTheme.typography.labelSmall,
                color = if (user?.isActive == true) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}

/** 设置列表项之间的分隔线（与标题文字左对齐）。 */
@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
