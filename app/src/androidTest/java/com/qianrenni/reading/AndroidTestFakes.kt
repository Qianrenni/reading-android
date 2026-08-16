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
import com.qianrenni.reading.data.model.LoginRequest
import com.qianrenni.reading.data.model.LoginResponse
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
import com.qianrenni.reading.data.remote.ShelfApi
import com.qianrenni.reading.data.remote.UserApi
import com.qianrenni.reading.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * androidTest（仪器化测试）专用测试替身。
 * 与 test（JVM 单测）源码集相互隔离，因此单独定义。
 */

class FakeAuthApi : AuthApi {
    var loginResult: NetworkResult<LoginResponse> = NetworkResult.Failure("n/a")
    var registerResult: NetworkResult<Unit> = NetworkResult.Empty()
    var verifyEmailResult: NetworkResult<Unit> = NetworkResult.Empty()
    var loginCalled = false
    var registerCalled = false
    var verifyEmailCalled = false
    var lastLoginRequest: LoginRequest? = null
    var lastRegisterRequest: RegisterRequest? = null
    var lastVerifyEmailRequest: EmailVerifyRequest? = null

    override suspend fun getCaptcha() = NetworkResult.Failure("n/a")

    override suspend fun login(request: LoginRequest, captchaId: String?): NetworkResult<LoginResponse> {
        loginCalled = true
        lastLoginRequest = request
        return loginResult
    }

    override suspend fun getCurrentUser() = NetworkResult.Failure("n/a")

    override suspend fun register(request: RegisterRequest): NetworkResult<Unit> {
        registerCalled = true
        lastRegisterRequest = request
        return registerResult
    }

    override suspend fun verifyEmail(request: EmailVerifyRequest): NetworkResult<Unit> {
        verifyEmailCalled = true
        lastVerifyEmailRequest = request
        return verifyEmailResult
    }
}

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

class FakeBookApi : BookApi {
    var categoriesResult: NetworkResult<List<String>> = NetworkResult.Success(emptyList())
    var booksResult: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray())
    var searchResult: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray())
    var bookResult: NetworkResult<Book> = NetworkResult.Failure("n/a")
    var catalogResult: NetworkResult<Array<Catalog>> = NetworkResult.Failure("n/a")
    var recommendResult: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray())
    var booksByIdsResult: NetworkResult<Array<Book>> = NetworkResult.Success(emptyArray())
    var getCategoriesCalled = false
    var searchCalled = false
    var lastSearchQuery: String? = null
    var lastCategory: String? = null
    var lastBookId: Int? = null

    override suspend fun getCategories(): NetworkResult<List<String>> {
        getCategoriesCalled = true
        return categoriesResult
    }

    override suspend fun getBooksByCategory(
        category: String,
        offset: Int,
        limit: Int,
        sort: String
    ): NetworkResult<Array<Book>> {
        lastCategory = category
        return booksResult
    }

    override suspend fun searchBooks(query: String): NetworkResult<Array<Book>> {
        searchCalled = true
        lastSearchQuery = query
        return searchResult
    }

    override suspend fun getBookById(bookId: Int): NetworkResult<Book> {
        lastBookId = bookId
        return bookResult
    }

    override suspend fun getCatalog(bookId: Int): NetworkResult<Array<Catalog>> = catalogResult
    override suspend fun getChapter(chapterId: Int, bookId: Int) = NetworkResult.Failure("n/a")
    override suspend fun getRecommendations(query: String) = recommendResult
    override suspend fun getBooksByIds(bookIds: List<Int>) = booksByIdsResult
}

class FakeUserApi : UserApi {
    var updatePasswordResult: NetworkResult<Unit> = NetworkResult.Empty()
    var sendCodeResult: NetworkResult<Unit> = NetworkResult.Empty()
    var resetPasswordResult: NetworkResult<Unit> = NetworkResult.Empty()
    var updatePasswordCalled = false
    var sendCodeCalled = false
    var resetPasswordCalled = false
    var lastUpdatePasswordRequest: UpdatePasswordRequest? = null
    var lastSendEmail: String? = null
    var lastResetRequest: ForgotPasswordRequest? = null

    override suspend fun updatePassword(request: UpdatePasswordRequest): NetworkResult<Unit> {
        updatePasswordCalled = true
        lastUpdatePasswordRequest = request
        return updatePasswordResult
    }

    override suspend fun sendForgotPasswordCode(userAccount: String): NetworkResult<Unit> {
        sendCodeCalled = true
        lastSendEmail = userAccount
        return sendCodeResult
    }

    override suspend fun resetPassword(request: ForgotPasswordRequest): NetworkResult<Unit> {
        resetPasswordCalled = true
        lastResetRequest = request
        return resetPasswordResult
    }
}

class FakeReadingProgressApi : ReadingProgressApi {
    var progressResult: NetworkResult<List<BookReadingProgress>> = NetworkResult.Success(emptyList())
    var updateResult: NetworkResult<Unit> = NetworkResult.Empty()
    var deleteResult: NetworkResult<Unit> = NetworkResult.Empty()
    var deleteCalled = false
    var lastDeletedBookId: Int? = null

    override suspend fun getReadingProgress() = progressResult
    override suspend fun updateReadingProgress(request: UpdateProgressRequest) = updateResult

    override suspend fun deleteReadingProgress(bookId: Int): NetworkResult<Unit> {
        deleteCalled = true
        lastDeletedBookId = bookId
        return deleteResult
    }
}

class FakeShelfApi : ShelfApi {
    var shelfResult: NetworkResult<List<ShelfItem>> = NetworkResult.Success(emptyList())
    var addResult: NetworkResult<Unit> = NetworkResult.Empty()
    var removeResult: NetworkResult<Unit> = NetworkResult.Empty()
    var addCalled = false
    var removeCalled = false
    var lastAddRequest: AddShelfRequest? = null
    var lastRemovedBookId: Int? = null

    override suspend fun getShelf() = shelfResult

    override suspend fun addToShelf(request: AddShelfRequest): NetworkResult<Unit> {
        addCalled = true
        lastAddRequest = request
        return addResult
    }

    override suspend fun removeFromShelf(bookId: Int): NetworkResult<Unit> {
        removeCalled = true
        lastRemovedBookId = bookId
        return removeResult
    }
}

class FakeCommentApi : CommentApi {
    var reviewsResult: NetworkResult<CommentPageResult> = NetworkResult.Success(CommentPageResult())
    var myReviewResult: NetworkResult<BookComment?> = NetworkResult.Empty()
    var createReviewResult: NetworkResult<Unit> = NetworkResult.Empty()
    var deleteReviewResult: NetworkResult<Unit> = NetworkResult.Empty()
    var createCalled = false
    var deleteCalled = false
    var lastCreatedContent: String? = null

    override suspend fun getBookReviews(bookId: Int, page: Int, size: Int) = reviewsResult
    override suspend fun getMyBookReview(bookId: Int) = myReviewResult

    override suspend fun createBookReview(bookId: Int, content: String): NetworkResult<Unit> {
        createCalled = true
        lastCreatedContent = content
        return createReviewResult
    }

    override suspend fun deleteBookReview(bookId: Int): NetworkResult<Unit> {
        deleteCalled = true
        return deleteReviewResult
    }

    override suspend fun getChapterComments(bookId: Int, chapterId: Int) =
        NetworkResult.Success(emptyMap<Int, List<BookChapterComment>>())

    override suspend fun createLineComment(bookId: Int, chapterId: Int, line: Int, content: String) =
        NetworkResult.Empty()

    override suspend fun deleteLineComment(bookId: Int, chapterId: Int, commentId: Int) = NetworkResult.Empty()
}

// ---- 便捷构造测试数据 ----

fun testUser(id: Int = 1, name: String = "tom") =
    User(id = id, userName = name, email = "$name@e.c", isActive = true)

fun testLoginResponse(access: String = "access", refresh: String = "refresh", user: User = testUser()) =
    LoginResponse(accessToken = access, refreshToken = refresh, tokenType = "Bearer", user = user)

fun testBook(id: Int = 1, name: String = "书$id", author: String = "作者") =
    Book(
        id = id,
        name = name,
        author = author,
        cover = "cover$id",
        description = "简介$id",
        category = "玄幻",
        tags = "热血",
        totalChapter = 10,
        createdAt = "2024-01-01T00:00:00"
    )

fun testCatalog(id: Int = 1, title: String = "第一章 启程") =
    Catalog(id = id, title = title, wordsCount = 100, order = id.toDouble())

fun testShelfItem(bookId: Int = 1, lastChapterId: Int? = 1, lastReadAt: String? = "2024-01-02T10:00:00") =
    ShelfItem(bookId = bookId, lastChapterId = lastChapterId, lastReadAt = lastReadAt)

fun testProgress(bookId: Int = 1, lastChapterId: Int = 1, lastReadAt: String = "2024-01-02T10:00:00") =
    BookReadingProgress(bookId = bookId, lastChapterId = lastChapterId, lastReadAt = lastReadAt)

fun testComment(id: Int = 1, bookId: Int = 1, userName: String = "读者", content: String = "很好看") =
    BookComment(
        id = id,
        bookId = bookId,
        userId = 99,
        userName = userName,
        content = content,
        createdAt = "2024-01-03T09:00:00"
    )
