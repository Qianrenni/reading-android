package com.qianrenni.reading.views.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.qianrenni.reading.Login
import com.qianrenni.reading.UpdatePassword
import com.qianrenni.reading.state.Navigator
import com.qianrenni.reading.viewmodels.auth.AuthViewModel

@Composable
fun ProfileView(
    navigator: Navigator,
    authViewModel: AuthViewModel = viewModel()
) {
    val user by authViewModel.getUser().collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "个人中心",
            style = MaterialTheme.typography.headlineMedium
        )

        // 用户信息卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头像
            AsyncImage(
                model = if (user?.avatar.isNullOrEmpty()) null else user?.avatar,
                contentDescription = "用户头像",
                modifier = Modifier.size(100.dp)
            )

            // 用户名
            Text(
                text = user?.userName ?: "未知用户",
                style = MaterialTheme.typography.titleLarge
            )

            // 邮箱
            Text(
                text = user?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 激活状态
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "状态:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (user?.isActive == true) "已激活" else "未激活",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (user?.isActive == true)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }

        // 修改密码按钮
        Button(
            onClick = {
                navigator.navigate(UpdatePassword)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("修改密码")
        }

        // 退出登录按钮
        Button(
            onClick = {
                authViewModel.clear()
                navigator.navigate(Login)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出登录")
        }
    }
}
