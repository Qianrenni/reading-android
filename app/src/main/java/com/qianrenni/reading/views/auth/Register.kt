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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qianrenni.reading.Login
import com.qianrenni.reading.components.CaptchaImage
import com.qianrenni.reading.state.Navigator
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.viewmodels.auth.RegisterViewModel
import kotlinx.coroutines.launch

@Composable
fun RegisterView(
    navigator: Navigator,
    viewModel: RegisterViewModel = viewModel()
) {
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(registerState.pageStatus.isError) {
        registerState.pageStatus.errorMessage?.let { error ->
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
            Text("新用户注册", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = registerState.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("用户名") },
                placeholder = { Text("请输入用户名") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = registerState.password,
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

            OutlinedTextField(
                value = registerState.confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChange(it) },
                label = { Text("确认密码") },
                placeholder = { Text("再次输入密码") },
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = registerState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text("邮箱") },
                    placeholder = { Text("请输入邮箱") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                Button(
                    onClick = {
                        viewModel.verifyEmail(
                            onSuccess = {
                                scope.launch {
                                    SnackBarManager.showMessage("验证邮件已发送，请到邮箱中验证")
                                }
                            }
                        )
                    },
                    enabled = !registerState.isVerifyingEmail,
                ) {
                    if (registerState.isVerifyingEmail) {
                        CircularProgressIndicator()
                    } else {
                        Text("验证邮箱")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = registerState.captcha,
                    onValueChange = { viewModel.onCaptchaChange(it) },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入验证码") },
                    singleLine = true,
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

            Button(
                onClick = {
                    viewModel.register(
                        onSuccess = {
                            scope.launch {
                                SnackBarManager.showMessage("注册成功，请登录")
                            }
                            navigator.navigate(Login)
                        }
                    )
                },
                enabled = !registerState.pageStatus.isLoading,
                modifier = Modifier.width(180.dp)
            ) {
                if (registerState.pageStatus.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("注册")
                }
            }

            TextButton(onClick = {
                navigator.navigate(Login)
            }) {
                Text(text = "已有账号？立即登录")
            }
        }
    }
}
