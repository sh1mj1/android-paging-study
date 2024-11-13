# Advanced Paging3

- `PagingSource`: PagingSource는 데이터의 스냅샷을 PagingData의 스트림으로 로드하기 위한 기본 클래스입니다.
- `PagingData`: 페이지로 나눈 데이터의 컨테이너. 데이터를 새로고침할 때마다 상응하는 PagingData가 별도로 생성됩니다.

새롭게 추가된 api

- `RemoteMediator`: 네트워크 및 데이터베이스에서 페이지로 나누기를 구현하는 데 유용합니다.
