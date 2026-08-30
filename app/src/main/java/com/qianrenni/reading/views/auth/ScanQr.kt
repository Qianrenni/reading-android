package com.qianrenni.reading.views.auth

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.qianrenni.reading.di.appContainer
import com.qianrenni.reading.navigation.Navigator
import com.qianrenni.reading.util.QrLoginParser
import com.qianrenni.reading.util.SnackBarManager
import com.qianrenni.reading.viewmodels.auth.AuthViewModel
import com.qianrenni.reading.viewmodels.auth.QrLoginViewModel

private const val TAG = "ScanQrView"

/**
 * 扫一扫登录网页端。
 *
 * 权限 → 相机取流（CameraX）→ ML Kit 端上识别二维码 →
 * 解析登录票据 → 上报 scan → 确认框（账号 + 网页端信息）→ confirm/cancel。
 */
@Composable
fun ScanQrView(
    navigator: Navigator,
    qrLoginViewModel: QrLoginViewModel = viewModel(factory = appContainer().viewModelFactory),
    authViewModel: AuthViewModel = viewModel(factory = appContainer().viewModelFactory)
) {
    val qrState by qrLoginViewModel.qrState.collectAsStateWithLifecycle()
    val user by authViewModel.getUser().collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 是否已识别到票据：置位后忽略后续帧，失败重新扫描时复位
    var scanned by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    // 确认成功/取消后给出反馈并返回个人中心
    LaunchedEffect(qrState.isConfirmed) {
        if (qrState.isConfirmed) {
            SnackBarManager.showMessage("登录成功,网页端即将跳转")
            navigator.goBack()
        }
    }
    LaunchedEffect(qrState.isCancelled) {
        if (qrState.isCancelled) {
            navigator.goBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(hasCameraPermission) {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                val mainExecutor = ContextCompat.getMainExecutor(context)
                var cameraProvider: ProcessCameraProvider? = null
                providerFuture.addListener({
                    try {
                        cameraProvider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(mainExecutor) { imageProxy ->
                            processFrame(barcodeScanner, imageProxy) { raw ->
                                val token = QrLoginParser.parseToken(raw)
                                if (token != null && !scanned) {
                                    scanned = true
                                    qrLoginViewModel.scan(token)
                                }
                            }
                        }
                        cameraProvider?.unbindAll()
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "相机绑定失败: ${e.message}")
                    }
                }, mainExecutor)
                onDispose {
                    barcodeScanner.close()
                    cameraProvider?.unbindAll()
                }
            }

            Text(
                text = "对准网页端登录二维码",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            )

            // 确认框：展示当前账号与网页端信息
            val client = qrState.client
            if (client != null && !qrState.isConfirmed) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("扫码登录确认") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("当前账号:${user?.userName ?: "未知用户"}")
                            Text("请求来源:$client")
                            Text("确认后网页端将登录该账号。")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { qrLoginViewModel.confirm() },
                            enabled = !qrState.isLoading
                        ) {
                            Text("确认登录")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { qrLoginViewModel.cancel() },
                            enabled = !qrState.isLoading
                        ) {
                            Text("取消")
                        }
                    }
                )
            }

            // 处理中/失败遮罩
            if (qrState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("正在处理…", color = Color.White)
                }
            }
            qrState.error?.let { error ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(error, color = Color.White)
                    Button(onClick = {
                        scanned = false
                        qrLoginViewModel.reset()
                    }) {
                        Text("重新扫描")
                    }
                }
            }
        } else if (permissionDenied) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("扫码登录需要使用相机,请在系统设置或下方按钮中授予权限。")
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("授予权限")
                }
                OutlinedButton(
                    onClick = { navigator.goBack() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("返回")
                }
            }
        } else {
            // 等待权限申请结果
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        }
    }
}

/** 用 ML Kit 识别单帧中的二维码，完成后必须 close(imageProxy) 释放缓冲 */
@OptIn(ExperimentalGetImage::class)
private fun processFrame(
    scanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onQrText: (String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val inputImage = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            onQrText(barcodes.firstNotNullOfOrNull { it.rawValue })
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
