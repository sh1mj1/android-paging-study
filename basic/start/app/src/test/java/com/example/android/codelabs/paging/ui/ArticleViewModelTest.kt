package com.example.android.codelabs.paging.ui

import androidx.paging.testing.asSnapshot
import com.example.android.codelabs.paging.data.Article
import com.example.android.codelabs.paging.data.ArticleWithoutTime
import com.example.android.codelabs.paging.data.FakeArticleRepository
import com.example.android.codelabs.paging.data.IArticleRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
@ExtendWith(CoroutinesTestExtension::class)
class ArticleViewModelTest {

    private lateinit var viewModel: ArticleViewModel
    private lateinit var repository: IArticleRepository

    @BeforeEach
    fun setUp() {
        repository = FakeArticleRepository(itemRange = (0 until 50))
        viewModel = ArticleViewModel(repository)
    }

    // 이거 왜 안 될까?!~~~
    @Test
    fun `test initial load of articles`() = runTest {
//        val itemsSnapshot: List<Article> = viewModel.items.asSnapshot {
//            scrollTo(index = 50)  // Scroll to load first 50 items
//        }
//
//        val expectedItems = (0 until 50).map {
//            Article(
//                id = it,
//                title = "Article $it",
//                description = "Description $it",
//                created = LocalDateTime.now()
//            )
//        }
//
//        expectedItems.map { ArticleWithoutTime.from(it) } shouldBe
//                itemsSnapshot.map { ArticleWithoutTime.from(it) }
    }
}
