package com.qianrenni.reading.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qianrenni.reading.ReadingApplication
import com.qianrenni.reading.data.remote.ApiClient
import com.qianrenni.reading.data.remote.AuthApi
import com.qianrenni.reading.data.remote.AuthApiImpl
import com.qianrenni.reading.data.remote.BookApi
import com.qianrenni.reading.data.remote.BookApiImpl
import com.qianrenni.reading.data.remote.CommentApi
import com.qianrenni.reading.data.remote.CommentApiImpl
import com.qianrenni.reading.data.remote.HttpClientFactory
import com.qianrenni.reading.data.remote.KtorTokenRefresher
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
import com.qianrenni.reading.data.repository.AuthRepository
import com.qianrenni.reading.data.repository.AuthRepositoryImpl
import com.qianrenni.reading.data.repository.EncryptedKeyValueStore
import com.qianrenni.reading.data.repository.SessionManager
import com.qianrenni.reading.data.repository.SharedPrefsKeyValueStore
import com.qianrenni.reading.data.repository.SettingsRepository
import com.qianrenni.reading.data.repository.SettingsRepositoryImpl
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.qianrenni.reading.viewmodels.auth.AuthViewModel
import com.qianrenni.reading.viewmodels.auth.ForgetPasswordViewModel
import com.qianrenni.reading.viewmodels.auth.LoginViewModel
import com.qianrenni.reading.viewmodels.auth.RegisterViewModel
import com.qianrenni.reading.viewmodels.auth.UpdatePasswordViewModel
import com.qianrenni.reading.viewmodels.book.BookInfoViewModel
import com.qianrenni.reading.viewmodels.book.BookReadViewModel
import com.qianrenni.reading.viewmodels.book.HistoryViewModel
import com.qianrenni.reading.viewmodels.book.HomeViewModel
import com.qianrenni.reading.viewmodels.book.ShelfViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 手动依赖注入容器（零第三方依赖）：
 * 持有全部单例依赖，并为 ViewModel 提供统一工厂。
 */
class AppContainer(private val context: Context) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // ---- 会话 / 配置 / 网络 ----
    val sessionManager: SessionManager = SessionManager(EncryptedKeyValueStore(context, "auth_prefs"))
    val appConfig: AppConfigRepository = AppConfigRepositoryImpl(SharedPrefsKeyValueStore(context, "app_config"))

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
    val bookApi: BookApi = BookApiImpl(apiClient)
    val commentApi: CommentApi = CommentApiImpl(apiClient)
    val readingProgressApi: ReadingProgressApi = ReadingProgressApiImpl(apiClient)
    val reportApi: ReportApi = ReportApiImpl(apiClient)
    val shelfApi: ShelfApi = ShelfApiImpl(apiClient)
    val userApi: UserApi = UserApiImpl(apiClient)

    // ---- Repository ----
    val authRepository: AuthRepository = AuthRepositoryImpl(sessionManager, authApi, tokenRefresher)

    // 阅读设置 DataStore（单例，进程级存活）
    private val settingsDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("read_settings") }
        )
    }
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(settingsDataStore)

    // ---- ViewModel 工厂（手动 DI）----
    val viewModelFactory = viewModelFactory {
        initializer { AuthViewModel(authRepository) }
        initializer { LoginViewModel(authApi, authRepository, ioDispatcher) }
        initializer { RegisterViewModel(authApi, ioDispatcher) }
        initializer { ForgetPasswordViewModel(userApi, ioDispatcher) }
        initializer { UpdatePasswordViewModel(userApi, authRepository, ioDispatcher) }
        initializer { HomeViewModel(bookApi, ioDispatcher) }
        initializer { BookInfoViewModel(bookApi, commentApi, ioDispatcher) }
        initializer { HistoryViewModel(bookApi, readingProgressApi, shelfApi, ioDispatcher) }
        initializer { ShelfViewModel(bookApi, readingProgressApi, shelfApi, ioDispatcher) }
        initializer { BookReadViewModel(bookApi, commentApi, readingProgressApi, reportApi, ioDispatcher) }
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
