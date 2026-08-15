package com.qianrenni.reading.views.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qianrenni.reading.ForgetPassword
import com.qianrenni.reading.Register
import com.qianrenni.reading.components.CaptchaImage
import com.qianrenni.reading.state.Navigator
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.viewmodels.auth.LoginViewModel

@Composable
fun LoginView(
    navigator: Navigator,
    viewModel: LoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.start()

    }
    LaunchedEffect(loginState.error) {
        loginState.error?.let { error ->
            SnackBarManager.showMessage(error)
        }
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("用户登录", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = loginState.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("邮箱") },
                placeholder = { Text("请输入用户邮箱") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = loginState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("密码") },
                placeholder = { Text("请输入密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp) // 输入框和图片之间的间距
            ) {
                OutlinedTextField(
                    value = loginState.captcha,
                    onValueChange = { viewModel.onCaptchaChange(it) },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入验证码") },
                    singleLine = true,
                    // 让输入框占据剩余空间
                    modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
                CaptchaImage(
                    modifier = Modifier
                        .width(120.dp)
                        .height(48.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = loginState.rememberMe,
                        onCheckedChange = { viewModel.onRememberMeChange(it) },
                    )
                    Text("记住我")
                }
                TextButton(onClick = {
                    navigator.navigate(ForgetPassword)
                }) {
                    Text(text = "忘记密码?")
                }
            }

            Button(
                onClick = {
                    viewModel.login()
                },
                enabled = !loginState.isLoading,
                modifier = Modifier.width(180.dp)
            ) {
                if (loginState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("登录")
                }
            }

            TextButton(onClick = {
                navigator.navigate(Register)
            }) {
                Text(text = "没有账号? 立即注册")
            }
        }
    }
}