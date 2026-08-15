package com.qianrenni.reading.views.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qianrenni.reading.state.Navigator
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.viewmodels.auth.UpdatePasswordViewModel

@Composable
fun UpdatePasswordView(
    navigator: Navigator,
    viewModel: UpdatePasswordViewModel = viewModel()
) {
    val updatePasswordState by viewModel.updatePasswordState.collectAsStateWithLifecycle()
    LaunchedEffect(updatePasswordState.pageStatus.errorMessage) {
        updatePasswordState.pageStatus.errorMessage?.let { error ->
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
            Text("修改密码", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = updatePasswordState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("邮箱") },
                placeholder = { Text("请输入邮箱") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = updatePasswordState.oldPassword,
                onValueChange = { viewModel.onOldPasswordChange(it) },
                label = { Text("旧密码") },
                placeholder = { Text("请输入旧密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = updatePasswordState.newPassword,
                onValueChange = { viewModel.onNewPasswordChange(it) },
                label = { Text("新密码") },
                placeholder = { Text("请输入新密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = updatePasswordState.confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChange(it) },
                label = { Text("确认密码") },
                placeholder = { Text("请再次输入新密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.updatePassword()
                },
                enabled = !updatePasswordState.pageStatus.isLoading,
                modifier = Modifier.width(180.dp)
            ) {
                if (updatePasswordState.pageStatus.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("修改密码")
                }
            }

            TextButton(onClick = {
                navigator.goBack()
            }) {
                Text(text = "返回")
            }
        }
    }
}
