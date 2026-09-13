package com.qianrenni.reading.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.navigation.Navigator


@Composable
fun CommonPage(
    uiState: CommonUiState,
    modifier: Modifier = Modifier,
    refresh: () -> Unit = {},
    navigator: Navigator? = null,
    // 默认跟随当前主题；阅读页等需要自定义纸张色时可传入主题色
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxSize(),
        color = containerColor
    ) {
        if (uiState.pageStatus.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.pageStatus.isError) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.pageStatus.errorMessage ?: "未知错误",
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = refresh) {
                    Text("重试")
                }
                Button(onClick = {
                    navigator?.goBack()
                }) {
                    Text("返回")
                }
            }
        } else {
            content()
        }
    }
}