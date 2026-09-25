package com.qianrenni.reading.views.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qianrenni.reading.di.appContainer
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.viewmodels.qr.QrScanViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 扫码登录网页端：扫描网页端登录页左侧的二维码，在手机上确认后网页端自动登录。
 *
 * 流程：相机取景 → 识别到本系统票据后上报后端（登记「待确认」）→ 弹出确认框 →
 * 用户确认后网页端轮询到 confirmed 并兑换令牌。
 *
 * 页面自带顶栏，因此已在 [com.qianrenni.reading.navigation.AppNavigation] 的
 * `routesWithoutPadding` 中登记，避免系统栏内边距被加两次。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanView(
    navigator: Navigator,
    viewModel: QrScanViewModel = viewModel(factory = appContainer().viewModelFactory)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val analyzer = remember { QrCodeAnalyzer(viewModel::onQrDetected) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    // 进入页面即申请权限，省去一次点击；拒绝后由页面上的按钮重新申请
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 已识别到票据后停止解码：相机每秒识别到同一张码多次，没必要反复跑识别
    SideEffect {
        analyzer.enabled = state.ticket.isEmpty() && !state.isConfirmed
    }

    // 一次性提示（无效二维码、网络错误等）
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            SnackBarManager.showMessage(message)
            viewModel.clearMessage()
        }
    }

    // 确认成功即完成使命：退回上一页（成功提示已在前一个副作用里发过，会继续显示）
    LaunchedEffect(state.isConfirmed) {
        if (state.isConfirmed) {
            navigator.goBack()
        }
    }

    // 相机生命周期：权限就绪后绑定预览 + 分析用例，离开页面或权限变化时解绑
    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        var cameraProvider: ProcessCameraProvider? = null
        val bindJob = scope.launch {
            if (!hasCameraPermission) {
                return@launch
            }
            val provider = try {
                awaitCameraProvider(context)
            } catch (error: Exception) {
                SnackBarManager.showMessage("相机启动失败：${error.message ?: "未知错误"}")
                return@launch
            }
            cameraProvider = provider
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (error: Exception) {
                SnackBarManager.showMessage("相机启动失败：${error.message ?: "未知错误"}")
            }
        }
        onDispose {
            bindJob.cancel()
            cameraProvider?.unbindAll()
        }
    }

    // 识别器与分析线程池随页面销毁释放
    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码登录网页端") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewView }
                )
                ScanFrameOverlay(isScanning = state.ticket.isEmpty())
            } else {
                CameraPermissionHint(
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (state.isConfirming) {
        AlertDialog(
            onDismissRequest = { viewModel.cancel() },
            title = { Text("确认登录网页端") },
            text = {
                Text(
                    "将使用当前账号「${viewModel.userName ?: "未知用户"}」登录网页端，确认后网页端会自动登录。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirm() },
                    enabled = !state.isSubmitting
                ) {
                    Text("确认登录")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancel() },
                    enabled = !state.isSubmitting
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/** 取景框提示：半透明遮罩 + 方框 + 底部文案。 */
@Composable
private fun ScanFrameOverlay(isScanning: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
                .border(width = 2.dp, color = Color.White)
        )
        Text(
            text = if (isScanning) "将网页端登录页的二维码放入框内" else "已识别，请在弹窗中确认",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}

/** 未授权相机时的兜底提示。 */
@Composable
private fun CameraPermissionHint(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要相机权限",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "扫码登录网页端需要使用相机扫描二维码",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onRequest,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("授予相机权限")
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

/** 把 CameraX 的 ListenableFuture 适配成挂起函数（避免额外引入 guava 适配依赖）。 */
private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { provider -> continuation.resume(provider) }
                    .onFailure { error -> continuation.resumeWithException(error) }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
