package com.qianrenni.reading.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.qianrenni.reading.R
import com.qianrenni.reading.data.api.AuthService
import kotlinx.coroutines.launch


@Composable
fun CaptchaImage(
    modifier: Modifier = Modifier
) {
    val captchaBytes = remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()
    val refreshCaptcha = {
        scope.launch {
            AuthService.getCaptcha().onSuccess {
                captchaBytes.value = it
            }
        }
    }
    LaunchedEffect(Unit) {
        refreshCaptcha()
    }
    Image(
        painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(captchaBytes.value)
                .size(Size.ORIGINAL)
                .build(),
            // 可选：添加占位/错误状态
            placeholder = painterResource(R.drawable.skeleton),
            error = painterResource(R.drawable.skeleton)
        ),
        contentDescription = "点击刷新验证码",
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .clickable(onClick = { refreshCaptcha() })
    )
}