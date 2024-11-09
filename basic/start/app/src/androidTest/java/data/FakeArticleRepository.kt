package data

import androidx.paging.PagingSource
import androidx.paging.testing.asPagingSourceFactory
import com.example.android.codelabs.paging.data.Article
import com.example.android.codelabs.paging.data.IArticleRepository
import java.time.LocalDateTime

class FakeArticleRepository(private val itemRange: IntRange) : IArticleRepository {
    private val items = itemRange.map { number ->
        Article(
            id = number,
            title = "Article $number",
            description = "This describes article $number",
            created = LocalDateTime.now(),
        )
    }

    private val pagingSourceFactory = items.asPagingSourceFactory()

    override fun articlePagingSource(): PagingSource<Int, Article> = pagingSourceFactory()
}