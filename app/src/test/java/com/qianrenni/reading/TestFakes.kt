package com.qianrenni.reading

import com.qianrenni.reading.data.model.AddShelfRequest
import com.qianrenni.reading.data.model.Book
import com.qianrenni.reading.data.model.BookChapterComment
import com.qianrenni.reading.data.model.BookComment
import com.qianrenni.reading.data.model.BookReadingProgress
import com.qianrenni.reading.data.model.Catalog
import com.qianrenni.reading.data.model.CommentPageResult
import com.qianrenni.reading.data.model.EmailVerifyRequest
import com.qianrenni.reading.data.model.ForgotPasswordRequest
import com.qianrenni.reading.data.model.LineCommentRequest
import com.qianrenni.reading.data.model.LoginRequest
import com.qianrenni.reading.data.model.LoginResponse
import com.qianrenni.reading.data.model.ReadEvent
import com.qianrenni.reading.data.model.RegisterRequest
import com.qianrenni.reading.data.model.ShelfItem
import com.qianrenni.reading.data.model.UpdatePasswordRequest
import com.qianrenni.reading.data.model.UpdateProgressRequest
import com.qianrenni.reading.data.model.User
import com.qianrenni.reading.data.remote.AuthApi
import com.qianrenni.reading.data.remote.BookApi
import com.qianrenni.reading.data.remote.CommentApi
import com.qianrenni.reading.data.remote.NetworkResult
import com.qianrenni.reading.data.remote.ReadingProgressApi
import com.qianrenni.reading.data.remote.ReportApi
import com.qianrenni.reading.data.remote.ShelfApi
import com.qianrenni.reading.data.remote.TokenRefresher
import com.qianrenni.reading.data.remote.UserApi
import com.qianrenni.reading.data.repository.AuthRepository
import com.qianrenni.reading.data.repository.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 内存版 KeyValueStore（用于存储层单元测试）。 */
class InMemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, String>()

    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) {
        map[key] = value
    }

    override fun remove(key: String) {
        map.remove(key)
    }
}

/** 内存版 AuthRepository（用于 ViewModel 单元测试）。 */
class FakeAuthRepository(initialUser: User? = null) : AuthRepository {
    val userFlow = MutableStateFlow(initialUser)
    override val user: StateFlow<User?> = userFlow
    override suspend fun initial() = Unit
    override fun setToken(accessToken: String, refreshToken: String, tokenType: String, isSave: Boolean) = Unit
    override fun setUser(user: User?) {
        userFlow.value = user
    }

    override fun clear() {
        userFlow.value = null
    }
}

/** 便捷构造测试用 User。 */
fun testUser(id: Int = 1, name: String = "tom") = User(id = id, userName = name, email = "$name@e.c", isActive = true)

/** 便捷构造测试用 LoginResponse。 */
fun testLoginResponse(access: String = "access", refresh: String = "refresh", user: User = testUser()) =
    LoginResponse(accessToken = access, refreshToken = refresh, tokenType = "Bearer", user = user)

/** 便捷构造测试用 Book。 */
fun testBook(id: Int = 1) = Book(id = id, name = "书$id", author = "作者", cover = "cover")

open class FakeAuthApi : AuthApi {
    var captchaResult: NetworkResult<ByteArray> = NetworkResult.Failure("n/a")
    var loginResult: NetworkResult<LoginResponse> = NetworkResult.Failure("n/a")
    var currentUserResult: NetworkResult<User> = NetworkResult.Failure("n/a")
    var registerResult: NetworkResult<Unit> = NetworkResult.Empty()
    var verifyEmailResult: NetworkResult<Unit> = NetworkResult.Empty()

    override suspend fun getCaptcha() = captchaResult
    override suspend fun login(request: LoginRequest, captchaId: String?) = loginResult
    override suspend fun getCurrentUser() = currentUserResult
    override suspend fun register(request: RegisterRequest) = registerResult
    override suspend fun verifyEmail(request: EmailVerifyRequest) = verifyEmailResult
}

open class FakeBookApi : BookApi {
    var categoriesResult: NetworkResult<List<String>> = NetworkResult.Success(emptyList())
    var booksResult: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray())
    var searchResult: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray())
    var bookResult: NetworkResult<Book> = NetworkResult.Failure("n/a")
    var catalogResult: NetworkResult<Array<Catalog>> = NetworkResult.Failure("n/a")
    var chapterResult: NetworkResult<String> = NetworkResult.Failure("n/a")
    var recommendResult: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray())
    var booksByIdsResult: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray())

    override suspend fun getCategories() = categoriesResult
    override suspend fun getBooksByCategory(category: String, offset: Int, limit: Int, sort: String) = booksResult
    override suspend fun searchBooks(query: String) = searchResult
    override suspend fun getBookById(bookId: Int) = bookResult
    override suspend fun getCatalog(bookId: Int) = catalogResult
    override suspend fun getChapter(chapterId: Int, bookId: Int) = chapterResult
    override suspend fun getRecommendations(query: String) = recommendResult
    override suspend fun getBooksByIds(bookIds: List<Int>) = booksByIdsResult
}

open class FakeCommentApi : CommentApi {
    var reviewsResult: NetworkResult<CommentPageResult> = NetworkResult.Success(CommentPageResult())
    var myReviewResult: NetworkResult<BookComment?> = NetworkResult.Empty()
    var createReviewResult: NetworkResult<Unit> = NetworkResult.Empty()
    var deleteReviewResult: NetworkResult<Unit> = NetworkResult.Empty()
    var chapterCommentsResult: NetworkResult<Map<Int, List<BookChapterComment>>> = NetworkResult.Empty()
    var createLineCommentResult: NetworkResult<Unit> = NetworkResult.Empty()
    var deleteLineCommentResult: NetworkResult<Unit> = NetworkResult.Empty()

    override suspend fun getBookReviews(bookId: Int, page: Int, size: Int) = reviewsResult
    override suspend fun getMyBookReview(bookId: Int) = myReviewResult
    override suspend fun createBookReview(bookId: Int, content: String) = createReviewResult
    override suspend fun deleteBookReview(bookId: Int) = deleteReviewResult
    override suspend fun getChapterComments(bookId: Int, chapterId: Int) = chapterCommentsResult
    override suspend fun createLineComment(bookId: Int, chapterId: Int, line: Int, content: String) = createLineCommentResult
    override suspend fun deleteLineComment(bookId: Int, chapterId: Int, commentId: Int) = deleteLineCommentResult
}

open class FakeReadingProgressApi : ReadingProgressApi {
    var progressResult: NetworkResult<List<BookReadingProgress>> = NetworkResult.Success(emptyList())
    var updateResult: NetworkResult<Unit> = NetworkResult.Empty()
    var deleteResult: NetworkResult<Unit> = NetworkResult.Empty()

    override suspend fun getReadingProgress() = progressResult
    override suspend fun updateReadingProgress(request: UpdateProgressRequest) = updateResult
    override suspend fun deleteReadingProgress(bookId: Int) = deleteResult
}

open class FakeReportApi : ReportApi {
    var reportResult: NetworkResult<Unit> = NetworkResult.Empty()

    override suspend fun reportChapterRead(event: ReadEvent) = reportResult
}

open class FakeShelfApi : ShelfApi {
    var shelfResult: NetworkResult<List<ShelfItem>> = NetworkResult.Success(emptyList())
    var addResult: NetworkResult<Unit> = NetworkResult.Empty()
    var removeResult: NetworkResult<Unit> = NetworkResult.Empty()

    override suspend fun getShelf() = shelfResult
    override suspend fun addToShelf(request: AddShelfRequest) = addResult
    override suspend fun removeFromShelf(bookId: Int) = removeResult
}

open class FakeUserApi : UserApi {
    var updatePasswordResult: NetworkResult<Unit> = NetworkResult.Empty()
    var sendCodeResult: NetworkResult<Unit> = NetworkResult.Empty()
    var resetPasswordResult: NetworkResult<Unit> = NetworkResult.Empty()

    override suspend fun updatePassword(request: UpdatePasswordRequest) = updatePasswordResult
    override suspend fun sendForgotPasswordCode(userAccount: String) = sendCodeResult
    override suspend fun resetPassword(request: ForgotPasswordRequest) = resetPasswordResult
}

class FakeTokenRefresher(var result: LoginResponse? = null) : TokenRefresher {
    var lastTokenType: String? = null
    var lastRefreshToken: String? = null
    var calls = 0

    override suspend fun refresh(tokenType: String, refreshToken: String): LoginResponse? {
        calls++
        lastTokenType = tokenType
        lastRefreshToken = refreshToken
        return result
    }
}
