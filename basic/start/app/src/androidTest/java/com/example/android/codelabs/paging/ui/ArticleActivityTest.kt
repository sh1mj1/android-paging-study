package com.example.android.codelabs.paging.ui

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.example.android.codelabs.paging.data.Article
import com.example.android.codelabs.paging.data.ArticleWithoutTime
import data.FakeArticleRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class ArticleActivityTest {

    @get:Rule
    val mainDispatcherRule = TestCoroutineRule()

    @Test
    fun defaultTest() = runTest {
        val expected = 1
        val actual = 1
        assertEquals(expected, actual)
    }

    private val viewModel = ArticleViewModel(
        repository = FakeArticleRepository(
            0 until 50
        )
    )

    @Test
    fun pagingTest(): Unit = runTest {
        val items: Flow<PagingData<Article>> = viewModel.items

        val itemsSnapshot: List<Article> = items.asSnapshot {
            scrollTo(index = 49)
        }

        val expected = (0 until 50).map { number ->
            Article(
                id = number,
                title = "Article $number",
                description = "This describes article $number",
                created = LocalDateTime.now(),
            )
        }

        assertEquals(
            expected.map { ArticleWithoutTime.from(it) },
            itemsSnapshot.map { ArticleWithoutTime.from(it) }
        )

        1 shouldBe 1

    }
}
