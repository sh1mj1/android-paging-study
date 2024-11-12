The PagingSource implementation defines the source of data and how to retrieve data from that
source.
The PagingData object queries data from the PagingSource in response to loading hints that are
generated as the user scrolls in a RecyclerView.

Currently, the GithubRepository has a lot of the responsibilities of a data source that the Paging
library will handle once we're done adding it:

Loads the data from GithubService, ensuring that multiple requests aren't triggered at the same
time.
Keeps an in-memory cache of the retrieved data.
Keeps track of the page to be requested.
To build the PagingSource you need to define the following:

The type of the paging key - in our case, the Github API uses 1-based index numbers for pages, so
the type is Int.
The type of data loaded - in our case, we're loading Repo items.
Where is the data retrieved from - we're getting the data from GithubService. Our data source will
be specific to a certain query,
so we need to make sure we're also passing the query information to GithubService.
So, in the data package, let's create a PagingSource implementation called GithubPagingSource:

페이징 소스는 데이터의 소스를 정의하고 해당 소스에서 데이터를 검색하는 방법을 정의합니다.
사용자가 리사이클러뷰 스크롤 -> 로딩 힌트 생성됨. -> 힌트에 대한 응답으로 PagingSource에서 데이터를 쿼리합니다.

현재는 GithubRepository has a lot of responsibilities of a data source of a data source that the Paging
Liberary will handle once we're doone adding it:

* Load the data from `GithubService`, ensuring that multiple request aren't triggered at the same
  time.
* Keep an in-memory cache of the retrieved data.
* Keep track of the page to be requested.

To Build the PagingSource you need to define the following:

* Type of Paging Key: in our case, Int.
* The type of data loaded: in out case, we're loading `Repo` type items.
* Where is the data retrived from: we're getting thd data from `GithubService`.
  we're passing the query information type is String. Our data source is specific to a certain
  query.

### load method for PagingSource

The LoadParam of the method `load(params: LoadParam<Int>): LoadResult<Int, Repo>` object keeps
information related to the load operation including:

* Key of the page to be loaded: At the first time that load is called, `LoadParam.key` is null.
  we're going to define the initial page key with `GITHUB_STARTING_PAGE_INDEX = 1`
* Load size: The requested number of items to load.

LoadResult.Page pass null for nextPage or prevPage, if the list can't be loaded in the corresponding
page.

### getRefreshKey

The refresh key is used for subsequent refresh calls to PagingSource.load().   
A refresh happens whenever the Paging Library wants to load new data to replace the current list.  
(e.g. on swipe to refresh or invalidation due to database updates, config changes, process death)  
Subsequent refresh calls will want to restart loading data centered around
PagingState.anchorPosition.  
it represents the most recently accessed index.

The GithubPagingSource implementation looks like this:
[GithubPagingSource.kt](../data/GithubPagingSource.kt)



