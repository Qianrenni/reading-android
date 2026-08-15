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
import com.qianrenni.reading.common.CommonUiState
import com.qianrenni.reading.state.Navigator


@Composable
fun CommonPage(
    uiState: CommonUiState,
    modifier: Modifier = Modifier,
    refresh: () -> Unit = {},
    navigator: Navigator? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
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