package com.qianrenni.reading.data.model

import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `User round trip`() {
        val user = User(1, "tom", "t@e.c", "avatar", true, listOf(1, 2))
        assertEquals(user, json.decodeFromString(User.serializer(), json.encodeToString(user)))
    }

    @Test
    fun `LoginResponse round trip`() {
        val resp = LoginResponse("a", "r", "Bearer", User(1, "u", "e", isActive = true))
        assertEquals(resp, json.decodeFromString(LoginResponse.serializer(), json.encodeToString(resp)))
    }

    @Test
    fun `ApiResponse wraps data`() {
        val api = ApiResponse<User>(code = 0, message = "ok", data = User(1, "u", "e", isActive = true))
        assertEquals(api, json.decodeFromString(ApiResponse.serializer(User.serializer()), json.encodeToString(api)))
        assertTrue(api.isSuccess)
        assertEquals(false, ApiResponse<User>().isSuccess)
    }

    @Test
    fun `Book and Catalog round trip`() {
        val book = Book(1, "n", "a", "c", "d", "cat", "tags", 10, "2026", true, 999)
        assertEquals(book, json.decodeFromString(Book.serializer(), json.encodeToString(book)))

        val catalog = Catalog(2, "t", 100, 1.5, "2026")
        assertEquals(catalog, json.decodeFromString(Catalog.serializer(), json.encodeToString(catalog)))
    }

    @Test
    fun `Comment models round trip`() {
        val comment = BookComment(1, 5, 9, "u", "", "内容", "ok", "2026", "2026", 0)
        assertEquals(comment, json.decodeFromString(BookComment.serializer(), json.encodeToString(comment)))

        val line = BookChapterComment(1, 3, 9, "u", "", "内容", "", "", "", null, 10)
        assertEquals(line, json.decodeFromString(BookChapterComment.serializer(), json.encodeToString(line)))

        val page = CommentPageResult(listOf(comment), 1, 1, 20)
        assertEquals(page, json.decodeFromString(CommentPageResult.serializer(), json.encodeToString(page)))

        assertEquals(BookReviewRequest("x"), json.decodeFromString(BookReviewRequest.serializer(), json.encodeToString(BookReviewRequest("x"))))
        assertEquals(LineCommentRequest(3, "y"), json.decodeFromString(LineCommentRequest.serializer(), json.encodeToString(LineCommentRequest(3, "y"))))
    }

    @Test
    fun `ReadingProgress models round trip`() {
        val progress = BookReadingProgress(1, 2, 3, "2026")
        assertEquals(progress, json.decodeFromString(BookReadingProgress.serializer(), json.encodeToString(progress)))

        val update = UpdateProgressRequest(1, 2, 3)
        assertEquals(update, json.decodeFromString(UpdateProgressRequest.serializer(), json.encodeToString(update)))

        val shelf = ShelfItem(1, "2026", 2, 3, "2026")
        assertEquals(shelf, json.decodeFromString(ShelfItem.serializer(), json.encodeToString(shelf)))

        assertEquals(AddShelfRequest(1), json.decodeFromString(AddShelfRequest.serializer(), json.encodeToString(AddShelfRequest(1))))
        assertEquals(ReadEvent(1, 2, "enter"), json.decodeFromString(ReadEvent.serializer(), json.encodeToString(ReadEvent(1, 2, "enter"))))
    }

    @Test
    fun `ReadSettings defaults and font families`() {
        val settings = ReadSettings(textColor = 1, backgroundColor = 2)
        assertEquals(18f, settings.fontSize)
        assertEquals(30f, settings.lineHeight)
        assertEquals(FontFamily.Default, settings.fontFamily)

        // 枚举与字体映射
        assertTrue(ReadFontFamily.entries.contains(ReadFontFamily.Default))
        assertEquals("默认", ReadFontFamily.Default.displayName)

        // 主题
        assertEquals("护眼", Themes.EyeTheme.label)
        assertEquals("纹理", Themes.PaperTheme.label)
    }
}
