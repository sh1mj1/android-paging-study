# Paging3 Overview

페이징(Paging) 라이브러리는 더 큰 데이터 세트에서 데이터 페이지를 로드하고 표시하는 데 도움이 됩니다.  
이 접근 방식을 사용하면 앱이 네트워크 대역폭과 시스템 리소스를 더 효율적으로 사용할 수 있습니다.  
페이징 라이브러리의 구성 요소는 권장되는 Android 앱 아키텍처에 맞게 설계되었습니다.
다른 제트팩(Jetpack) 컴포넌트들과 깔끔하게 통합됩니다.
코틀린 1급 클래스(first class) 를 지원합니다.

## 페이징 라이브러리 사용의 이점

페이징 라이브러리를 사용하면 다음과 같은 기능을 활용할 수 있습니다:

* 페이징된 데이터를 처리하는 데 도움이 되는 메모리 내 캐싱.
    * 이를 통해 앱이 페이징된 데이터를 처리하는 동안 시스템 리소스를 효율적으로 사용할 수 있습니다.
* 내장된 요청 중복 제거 기능.
    * 이를 통해 앱이 네트워크 대역폭과 시스템 리소스를 효율적으로 사용할 수 있습니다.
* 사용자가 로드된 데이터의 끝으로 스크롤할 때 자동으로 데이터를 요청하는 구성 가능한 RecyclerView 어댑터.
* 코틀린 코루틴 및 플로우, LiveData 및 RxJava에 대한 1급 클래스 지원
* 새로 고침 및 다시 시도 기능을 포함한 내장된 오류 처리 지원.

```toml
androidxPaging = "3.3.2"

# alternatively - without Android dependencies for tests
androidx-paging-runtime-ktx = { group = "androidx.paging", name = "paging-runtime-ktx", version.ref = "androidxPaging" }

androidx-paging-common-ktx = { group = "androidx.paging", name = "paging-common-ktx", version.ref = "androidxPaging" }

```

```groovy 
implementation libs.androidx.paging.common.ktx
implementation libs.androidx.paging.runtime.ktx
```

## 라이브러리 아키텍처

페이징 라이브러리의 컴포넌트들은 앱의 세 레이어에서 동작합니다.

* 레포지토리(repository) 레이어
* 뷰모델(ViewModel) 레이어
* UI 레이어

![img.png](img.png)

각 레이어에서 동작하는 페이징 라이브러리 컴포넌트와 페이징된 데이터를 로드하고 표시하는 방법에 대해 설명하겠습니다.

### 레포지토리(repository) 레이어

주요한 레포지토리 레이어에 있는 페이징 라이브러리 컴포넌트는 PagingSource(페이징 소스) 입니다.  
각 페이징 소스 객체는 데이터의 소스와 그 소스에서 데이터를 검색하는 방법을 정의합니다.  
페이징 소스 객체는 네트워크 소스와 로컬 데이터베이스를 포함한 모든 단일 소스에서 데이터를 로드할 수 있습니다.

또 다른 페이징 라이브러리 컴포넌트로 RemoteMediator 를 사용할 수도 있습니다.  
RemoteMediator 객체는 로컬 데이터베이스 캐시와 같은 계층화된 데이터 소스에서 페이징을 처리합니다.

### 뷰모델(ViewModel) 레이어

페이징 컴포넌트는 페이징 데이터의 인스턴스를 구성하는 API 를 제공합니다.  
이 PagingData(페이징 데이터)는 반응형(reactive) 스트림에 노출되며, PagingSource 객체와 PagingConfig 구성 객체를 기반으로 합니다.

뷰모델 레이어를 UI 에 연결하는 컴포넌트는 PagingData 입니다.  
페이징 데이터 객체는 페이징된 데이터의 스냅샷을 포함하는 컨테이너입니다.  
PagingSource 객체를 쿼리하고 결과를 저장합니다.

### UI 레이어

UI 레이어의 주요 페이징 라이브러리 컴포넌트는 PagingDataAdapter 입니다.  
PagingDataAdapter 는 페이지별 데이터를 처리하는 RecyclerView 어댑터입니다.
대신에 AsyncPagingDataDiffer 컴포넌트를 사용하여 사용자 정의 어댑터를 만들 수 있습니다.

> 만약 UI 에서 컴포즈를 사용한다면, androidx.paging:paging-compose 라는 아티팩트를 사용할 수 있습니다.

## 페이지네이션(Pagination) 소개

유저에게 정보를 제공하는 가장 일반적인 방법 중 하나는 리스트를 사용하는 것입니다.
사용자가 리스트를 스크롤할 때 더 많은 데이터가 자동으로 로드될 것으로 예상됩니다.  
데이터가 가져올 때마다 효율적이고 원활해야 하며, 점진적인 로드가 UX(사용자 경험)을 해치지 않아야 합니다.
점진적인 로드는 앱이 한 번에 많은 양의 데이터를 메모리에 보유할 필요가 없어집니다.
즉, 성능상의 이점을 제공합니다.

이렇게 점진적으로 정볼르 가져오는 프로세스를 페이지네이션(Pagination) 이라고 합니다.
각 페이지는 가져올 데이터의 청크(chunk)에 해당합니다.
페이지를 요청하려면, 페이징되는 데이터 소스는 종종 필요한 정보를 정의하는 쿼리가 필요합니다.
이제 간단히 페이징을 구현해봅시다.

## 페이징 라이브러리의 코어 컴포넌트들

PagingSource - the base class for loading chunks of data for a specific page query. It is part of
the data layer, and is typically exposed from a DataSource class and subsequently by the Repository
for use in the ViewModel.
PagingConfig - a class that defines the parameters that determine paging behavior. This includes
page size, whether placeholders are enabled, and so on.
Pager - a class responsible for producing the PagingData stream. It depends on the PagingSource to
do this and should be created in the ViewModel.
PagingData - a container for paginated data. Each refresh of data will have a separate corresponding
PagingData emission backed by its own PagingSource.
PagingDataAdapter - a RecyclerView.Adapter subclass that presents PagingData in a RecyclerView. The
PagingDataAdapter can be connected to a Kotlin Flow, a LiveData, an RxJava Flowable, an RxJava
Observable, or even a static list using factory methods. The PagingDataAdapter listens to internal
PagingData loading events and efficiently updates the UI as pages are loaded.

* `PagingSource`
    * 특정 페이지 쿼리에 대한 데이터 청크를 로드하는 기본 클래스입니다.
    * 데이터 레이어의 일부이며, 데이터 소스(DataSource) 클래스에서 노출되고, 이후에 뷰모델에서 사용하기 위해 노출됩니다.
* `PagingConfig`
    * 페이징 동작을 결정하는 매개변수를 정의하는 클래스입니다.
    * 페이지 크기, 플레이스홀더 활성화 여부 등을 포함합니다.
* `Pager`
    * `PagingData` 스트림을 생성하는 책임을 지는 클래스입니다.
    * 이를 수행하기 위해 `PagingSource`에 의존하며, 뷰모델에서 생성되어야 합니다.
* `PagingData`
    * 페이지별 데이터를 위한 컨테이너입니다.
    * 데이터의 각 새로 고침은 자체 `PagingSource`에 의해 지원되는 별도의 `PagingData` 방출을 가집니다.
* `PagingDataAdapter`
    * 리사이클러뷰(RecyclerView)에 `PagingData`를 제공하는 `RecyclerView.Adapter` 하위 클래스입니다.
    * `PagingDataAdapter`는 Kotlin `Flow`, `LiveData`, `RxJava Flowable`, `RxJava Observable` 또는 팩토리
      메서드를 사용하여 정적 목록에 연결될 수 있습니다.
    * `PagingDataAdapter`는 내부 `PagingData` 로딩 이벤트를 수신하고 페이지가 로드될 때 효율적으로 UI를 업데이트합니다.

![img_1.png](img_1.png)

## Practice

To build the `PagingSource` you will need to define the following:

* The type of the paging key - The definition of the type of the page query we use to request more
  data.
  In our case, we fetch articles after or before a certain article ID since the IDs are guaranteed
  to be ordered and increasing.
* The type of data loaded - Each page returns a List of articles, so the type is Article.
* Where the data is retrieved from - Typically, this would be a database, network resource, or any
  other source of paginated data. In the case of this codelab however, we're using locally generated
  data.
* 페이징 키의 타입 - 더 많은 데이터를 요청하는 데 사용되는 페이지 쿼리의 타입을 정의합니다.
  우리의 경우, ID가 정렬되고 증가된다는 것이 보장됩니다. 그러므로 특정 게시물 ID 이후 또는 이전의 게시물을 가져옵니다.
* 로드되는 데이터 타입 - 각 페이지는 게시물 목록을 반환하므로 타입은 게시물(Article)입니다.
* 데이터가 검색되는 곳 - 일반적으로 데이터베이스, 네트워크 리소스 또는 페이지별 데이터의 다른 소스가 될 수 있습니다.
  그러나 이 코드랩의 경우, 로컬로 생성된 데이터를 사용합니다.



