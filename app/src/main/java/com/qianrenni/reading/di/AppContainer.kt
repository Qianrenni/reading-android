package com.qianrenni.reading.di

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qianrenni.reading.ReadingApplication
import com.qianrenni.reading.bridge.AuthBridgeModule
import com.qianrenni.reading.bridge.BridgeModule
import com.qianrenni.reading.bridge.BundleBridgeModule
import com.qianrenni.reading.bridge.DeviceBridgeModule
import com.qianrenni.reading.bridge.JsEvaluatorHolder
import com.qianrenni.reading.bridge.NativeBridge
import com.qianrenni.reading.bridge.NetworkBridgeModule
import com.qianrenni.reading.bridge.RouteBridgeModule
import com.qianrenni.reading.bridge.UiBridgeModule
import com.qianrenni.reading.bridge.rn.GugaReactPackage
import com.qianrenni.reading.data.remote.ApiClient
import com.qianrenni.reading.data.remote.AuthApi
import com.qianrenni.reading.data.remote.AuthApiImpl
import com.qianrenni.reading.data.remote.BookApi
import com.qianrenni.reading.data.remote.BookApiImpl
import com.qianrenni.reading.data.remote.BundleApi
import com.qianrenni.reading.data.remote.BundleApiImpl
import com.qianrenni.reading.data.remote.CommentApi
import com.qianrenni.reading.data.remote.CommentApiImpl
import com.qianrenni.reading.data.remote.DeviceLoginApi
import com.qianrenni.reading.data.remote.DeviceLoginApiImpl
import com.qianrenni.reading.data.remote.HttpClientFactory
import com.qianrenni.reading.data.remote.KtorTokenRefresher
import com.qianrenni.reading.data.remote.QrLoginApi
import com.qianrenni.reading.data.remote.QrLoginApiImpl
import com.qianrenni.reading.data.remote.ReadingProgressApi
import com.qianrenni.reading.data.remote.ReadingProgressApiImpl
import com.qianrenni.reading.data.remote.ReportApi
import com.qianrenni.reading.data.remote.ReportApiImpl
import com.qianrenni.reading.data.remote.ShelfApi
import com.qianrenni.reading.data.remote.ShelfApiImpl
import com.qianrenni.reading.data.remote.TokenRefresher
import com.qianrenni.reading.data.remote.UserApi
import com.qianrenni.reading.data.remote.UserApiImpl
import com.qianrenni.reading.data.repository.AppConfigRepository
import com.qianrenni.reading.data.repository.AppConfigRepositoryImpl
import com.qianrenni.reading.data.repository.AppUpdateRepository
import com.qianrenni.reading.data.repository.AppUpdateRepositoryImpl
import com.qianrenni.reading.data.repository.AuthRepository
import com.qianrenni.reading.data.repository.AuthRepositoryImpl
import com.qianrenni.reading.data.repository.BiometricLoginRepository
import com.qianrenni.reading.data.repository.BiometricLoginRepositoryImpl
import com.qianrenni.reading.data.repository.EncryptedKeyValueStore
import com.qianrenni.reading.data.repository.HybridBundleRepository
import com.qianrenni.reading.data.repository.HybridBundleRepositoryImpl
import com.qianrenni.reading.data.repository.KtorFileDownloader
import com.qianrenni.reading.data.repository.SessionManager
import com.qianrenni.reading.data.repository.SharedPrefsKeyValueStore
import com.qianrenni.reading.data.repository.SettingsRepository
import com.qianrenni.reading.data.repository.SettingsRepositoryImpl
import com.qianrenni.reading.data.repository.ThemeRepository
import com.qianrenni.reading.data.repository.ThemeRepositoryImpl
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.qianrenni.reading.viewmodels.app.AppUpdateViewModel
import com.qianrenni.reading.viewmodels.auth.AuthViewModel
import com.qianrenni.reading.viewmodels.auth.BiometricViewModel
import com.qianrenni.reading.viewmodels.auth.ForgetPasswordViewModel
import com.qianrenni.reading.viewmodels.auth.LoginViewModel
import com.qianrenni.reading.viewmodels.auth.RegisterViewModel
import com.qianrenni.reading.viewmodels.auth.UpdatePasswordViewModel
import com.qianrenni.reading.viewmodels.book.BookInfoViewModel
import com.qianrenni.reading.viewmodels.book.BookReadViewModel
import com.qianrenni.reading.viewmodels.book.HistoryViewModel
import com.qianrenni.reading.viewmodels.book.HomeViewModel
import com.qianrenni.reading.viewmodels.book.ShelfViewModel
import com.qianrenni.reading.viewmodels.qr.QrScanViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.qianrenni.reading.hybrid.BuiltinRouteManifest
import com.qianrenni.reading.hybrid.H5Engine
import com.qianrenni.reading.hybrid.HybridEngineRegistry
import com.qianrenni.reading.hybrid.HybridPageSpec
import com.qianrenni.reading.hybrid.RnEngine
import com.qianrenni.reading.hybrid.RnRuntime
import com.qianrenni.reading.navigation.Bookshelf
import com.qianrenni.reading.navigation.History
import com.qianrenni.reading.navigation.Home
import com.qianrenni.reading.navigation.NativeRouteRegistry
import com.qianrenni.reading.navigation.NavigatorHolder
import com.qianrenni.reading.navigation.PrivacyPolicy
import com.qianrenni.reading.navigation.Profile
import com.qianrenni.reading.util.BiometricAuth
import com.qianrenni.reading.util.AndroidApkReader
import com.qianrenni.reading.util.installedAppVersion
import java.io.File

/**
 * 手动依赖注入容器（零第三方依赖）：
 * 持有全部单例依赖，并为 ViewModel 提供统一工厂。
 */
class AppContainer(private val context: Context) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /** 更新包在静态目录中的相对路径。 */
    private val apkPath = APK_PATH

    // ---- 会话 / 配置 / 网络 ----
    val sessionManager: SessionManager = SessionManager(EncryptedKeyValueStore(context, "auth_prefs"))
    val appConfig: AppConfigRepository = AppConfigRepositoryImpl(SharedPrefsKeyValueStore(context, "app_config"))
    // 主题外观（跟随系统 / 白天 / 黑夜）
    val themeRepository: ThemeRepository =
        ThemeRepositoryImpl(SharedPrefsKeyValueStore(context, "app_theme"))

    // 裸客户端：用于令牌刷新（避免 Auth 递归）
    private val bareClient: HttpClient = HttpClientFactory.createBareClient()
    private val tokenRefresher: TokenRefresher =
        KtorTokenRefresher(bareClient, { appConfig.currentBaseUrl() })

    // 主客户端：Ktor Auth(Bearer) 自动注入令牌、401 自动刷新并重试
    private val authClient: HttpClient = HttpClientFactory.createAuthClient(
        onLoadTokens = { sessionManager.bearerTokens() },
        onRefreshTokens = {
            val saved = sessionManager.tokens() ?: return@createAuthClient null
            val refreshed = tokenRefresher.refresh(saved.tokenType, saved.refreshToken)
                ?: return@createAuthClient null
            sessionManager.setToken(refreshed.accessToken, refreshed.refreshToken, refreshed.tokenType)
            sessionManager.bearerTokens()
        }
    )

    private val apiClient: ApiClient = ApiClient(
        client = authClient,
        baseUrlProvider = { appConfig.currentBaseUrl() },
        onUnauthorized = { sessionManager.clear() }
    )

    // ---- API（接口注入，便于测试替换为 Fake）----
    val authApi: AuthApi = AuthApiImpl(apiClient)
    val qrLoginApi: QrLoginApi = QrLoginApiImpl(apiClient)
    // 设备凭据登录：签发/撤销需要登录态（apiClient），换取令牌必须绕过 Auth 插件（bareClient）
    val deviceLoginApi: DeviceLoginApi =
        DeviceLoginApiImpl(apiClient, bareClient) { appConfig.currentBaseUrl() }
    val bookApi: BookApi = BookApiImpl(apiClient)
    val commentApi: CommentApi = CommentApiImpl(apiClient)
    val readingProgressApi: ReadingProgressApi = ReadingProgressApiImpl(apiClient)
    val reportApi: ReportApi = ReportApiImpl(apiClient)
    val shelfApi: ShelfApi = ShelfApiImpl(apiClient)
    val userApi: UserApi = UserApiImpl(apiClient)

    // ---- Repository ----
    val authRepository: AuthRepository = AuthRepositoryImpl(sessionManager, authApi, tokenRefresher)
    // 指纹解锁登录：设备凭据单独存一份（不随退出登录清除），必须指纹校验后才取出使用
    val biometricRepository: BiometricLoginRepository = BiometricLoginRepositoryImpl(
        store = EncryptedKeyValueStore(context, "biometric_prefs"),
        deviceLoginApi = deviceLoginApi
    )

    // ---- 应用更新：更新包由后端静态目录提供（{baseUrl}static/guga.apk）----
    val appUpdateRepository: AppUpdateRepository = AppUpdateRepositoryImpl(
        currentVersionProvider = { installedAppVersion(context) },
        apkUrlProvider = { appConfig.currentBaseUrl() + apkPath },
        apkFileProvider = { File(context.cacheDir, "app_update/$APK_FILE_NAME") },
        downloader = KtorFileDownloader(bareClient),
        apkReader = AndroidApkReader(context),
        expectedPackageName = context.packageName,
        ioDispatcher = ioDispatcher
    )

    // 阅读设置 DataStore（单例，进程级存活）
    private val settingsDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("read_settings") }
        )
    }
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(settingsDataStore)

    // ---- 容器（native / h5 / rn 三引擎 + 页面包分发）----

    /** 桥与 RN 原生模块的异步作用域：页面调用可能跨越组合生命周期，故随进程存活。 */
    private val hybridScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jsEvaluatorHolder = JsEvaluatorHolder()

    /** 组合建立后由 AppNavigation 注入，供桥的路由能力使用。 */
    val navigatorHolder = NavigatorHolder()

    /** 纳入容器路由表的 native 页面：上传 h5/rn 包即可把同名 route 切过去，无需发版。 */
    val nativeRoutes = NativeRouteRegistry(
        mapOf(
            "home" to Home,
            "bookshelf" to Bookshelf,
            "history" to History,
            "profile" to Profile,
            "privacy" to PrivacyPolicy,
        )
    )

    val bundleApi: BundleApi = BundleApiImpl(apiClient)

    val hybridBundleRepository: HybridBundleRepository = HybridBundleRepositoryImpl(
        bundleApi = bundleApi,
        downloader = KtorFileDownloader(bareClient),
        rootDir = File(context.filesDir, HYBRID_DIR),
        appVersionCodeProvider = { installedAppVersion(context).versionCode.toInt() },
        ioDispatcher = ioDispatcher,
    )

    /** 随包发布的内置路由（`assets/hybrid/builtin.json`）；缺文件/格式错按空处理，不影响启动。 */
    val builtinHybridRoutes: List<HybridPageSpec> = runCatching {
        context.assets.open(BUILTIN_ROUTE_ASSET).bufferedReader().use { it.readText() }
    }.map(BuiltinRouteManifest::parse).getOrElse { emptyList() }

    private val bridgeModules: List<BridgeModule> = listOf(
        AuthBridgeModule(authRepository, sessionManager),
        RouteBridgeModule(navigatorHolder),
        DeviceBridgeModule(
            appVersionName = { installedAppVersion(context).versionName },
            appVersionCode = { installedAppVersion(context).versionCode.toInt() },
        ),
        UiBridgeModule(),
        BundleBridgeModule(hybridBundleRepository),
        NetworkBridgeModule(apiClient),
    )

    /** 只注入容器页面（自研 h5 包）：通用 H5 容器不注入，避免登录态暴露给第三方网页。 */
    private val nativeBridge = NativeBridge(
        modules = bridgeModules,
        scope = hybridScope,
        evaluatorHolder = jsEvaluatorHolder,
    )

    val hybridEngines: HybridEngineRegistry = HybridEngineRegistry(
        listOf(
            H5Engine(bridge = nativeBridge, evaluatorHolder = jsEvaluatorHolder),
            RnEngine(
                runtime = RnRuntime(context.applicationContext as Application),
                packages = listOf(GugaReactPackage(bridgeModules, hybridScope)),
                themeModeProvider = { themeRepository.mode.value },
            ),
        )
    )

    // ---- ViewModel 工厂（手动 DI）----
    val viewModelFactory = viewModelFactory {
        initializer { AuthViewModel(authRepository) }
        initializer { LoginViewModel(authApi, authRepository, ioDispatcher) }
        initializer { RegisterViewModel(authApi, ioDispatcher) }
        initializer { ForgetPasswordViewModel(userApi, ioDispatcher) }
        initializer { UpdatePasswordViewModel(userApi, authRepository, ioDispatcher) }
        initializer { HomeViewModel(bookApi, ioDispatcher) }
        initializer { BookInfoViewModel(bookApi, commentApi, ioDispatcher) }
        initializer {
            BiometricViewModel(
                biometricRepository = biometricRepository,
                authRepository = authRepository,
                // 设备能力探测注入到 VM，避免 VM 直接依赖 Android 框架
                isBiometricAvailable = { BiometricAuth.isAvailable(context) },
                ioDispatcher = ioDispatcher
            )
        }
        initializer { HistoryViewModel(bookApi, readingProgressApi, shelfApi, ioDispatcher) }
        initializer { ShelfViewModel(bookApi, readingProgressApi, shelfApi, ioDispatcher) }
        initializer { BookReadViewModel(bookApi, commentApi, readingProgressApi, reportApi, ioDispatcher) }
        initializer { AppUpdateViewModel(appUpdateRepository, ioDispatcher) }
        initializer { QrScanViewModel(qrLoginApi, authRepository, bundleApi, hybridBundleRepository, ioDispatcher) }
    }

    private companion object {
        /** 后端静态目录中的更新包文件名。 */
        const val APK_FILE_NAME = "guga.apk"
        const val APK_PATH = "static/$APK_FILE_NAME"

        /** 已安装页面包的落盘目录（应用私有目录）。 */
        const val HYBRID_DIR = "hybrid"

        /** 随包发布的内置容器路由清单。 */
        const val BUILTIN_ROUTE_ASSET = "hybrid/builtin.json"
    }
}

/**
 * 在 Compose 中获取全局容器。
 */
@Composable
fun appContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as ReadingApplication).container
}
