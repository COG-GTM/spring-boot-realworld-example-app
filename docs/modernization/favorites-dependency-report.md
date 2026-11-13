# Favorites feature – dependency report

Scope: every place the "favorite an article" feature touches the **articles** and **users** domains in this
Spring Boot 2.6 / MyBatis / REST + GraphQL (Netflix DGS) codebase. All references are to `master` at the time
of writing (`path:line`). Nothing in this report changes runtime behaviour.

## TL;DR

- The feature owns very little code of its own: one aggregate (`ArticleFavorite`), one repository, one MyBatis
  mapper, one read service and one table (`article_favorites`).
- It is coupled to articles and users mostly **through SQL**, not through Java types: four MyBatis statements
  join `article_favorites` to `articles` and `users`, and the article read model (`ArticleData`) carries
  `favorited` / `favoritesCount` which are filled in by `ArticleQueryService`.
- Both API layers (REST and GraphQL) write favorites directly via `ArticleFavoriteRepository` and resolve the
  target article via `ArticleRepository.findBySlug`, then re-read the article through `ArticleQueryService`.
- There are no database foreign keys and no cascading delete: deleting an article leaves orphan favorite rows.
- GraphQL favorite mutations and the `Profile.favorites` field have **no tests**.

## 1. Data model (schema)

| Ref | What | Coupling |
| --- | --- | --- |
| `src/main/resources/db/migration/V1__create_tables.sql:21-25` | `article_favorites(article_id, user_id)`, composite PK | `article_id` → `articles.id` (`:10-19`), `user_id` → `users.id` (`:1-8`). **Logical FKs only – no `REFERENCES`, no `ON DELETE CASCADE`.** |

## 2. Domain / core

| Ref | What | Coupling |
| --- | --- | --- |
| `src/main/java/io/spring/core/favorite/ArticleFavorite.java:10-17` | Aggregate holding `articleId`, `userId` (plain `String`s) | Identity is the pair of article id + user id. `@EqualsAndHashCode` is relied upon by `ArticleFavoriteApiTest:101`. |
| `src/main/java/io/spring/core/favorite/ArticleFavoriteRepository.java:5-11` | `save`, `find(articleId, userId)`, `remove` | Port used directly by both API layers (see §5, §6). |

## 3. Persistence – MyBatis (Java interfaces + XML)

### 3.1 Write side (owned by favorites)

| Ref | What | Coupling |
| --- | --- | --- |
| `src/main/java/io/spring/infrastructure/repository/MyBatisArticleFavoriteRepository.java:10-35` | `ArticleFavoriteRepository` impl | `save` is idempotent via find-then-insert (`:20-24`) – not atomic; relies on the composite PK to reject races. |
| `src/main/java/io/spring/infrastructure/mybatis/mapper/ArticleFavoriteMapper.java:7-14` | Mapper interface | Bound by namespace to the XML below. |
| `src/main/resources/mapper/ArticleFavoriteMapper.xml:4-6` | `insert` into `article_favorites` | Table only. |
| `src/main/resources/mapper/ArticleFavoriteMapper.xml:7-9` | `delete` by `(article_id, user_id)` | Table only. |
| `src/main/resources/mapper/ArticleFavoriteMapper.xml:10-20` | `find` + `resultMap articleFavorite` → `io.spring.core.favorite.ArticleFavorite` | XML hard-codes the core class FQN (`:17`). |

### 3.2 Read side used to decorate articles (owned by favorites, consumed by articles)

| Ref | What | Coupling |
| --- | --- | --- |
| `src/main/java/io/spring/infrastructure/mybatis/readservice/ArticleFavoritesReadService.java:10-19` | `isUserFavorite`, `articleFavoriteCount`, `articlesFavoriteCount`, `userFavorites` | `userFavorites` takes a **`io.spring.core.user.User`** object (`:18`) rather than an id. |
| `src/main/resources/mapper/ArticleFavoritesReadService.xml:4-6` | `isUserFavorite` – `count(1)` on `article_favorites` | Table only. |
| `src/main/resources/mapper/ArticleFavoritesReadService.xml:7-9` | `articleFavoriteCount` | Table only. |
| `src/main/resources/mapper/ArticleFavoritesReadService.xml:10-18` | `articlesFavoriteCount` – `articles A left join article_favorites AF` | **Joins `articles`.** Uses `resultMap="transfer.data.favoriteCount"` defined in another file. |
| `src/main/resources/mapper/ArticleFavoritesReadService.xml:19-29` | `userFavorites` – `articles A left join article_favorites AF ... AF.user_id = #{currentUser.id}` | **Joins `articles`**, reads property `id` off the `User` domain object. |
| `src/main/resources/mapper/TransferData.xml:4-9` | `resultMap favoriteCount` → `io.spring.application.data.ArticleFavoriteCount` (constructor mapping `id`, `favoriteCount`) | Shared "transfer.data" namespace also holds article/profile/comment maps. |
| `src/main/java/io/spring/application/data/ArticleFavoriteCount.java:5-9` | DTO `(id, count)` | Constructor arg order must match `TransferData.xml:6-7`. |

### 3.3 Favorites embedded in *article* queries (owned by articles)

The `favoritedBy` filter (articles favorited by a given **username**) is implemented inside the article read
service, joining favorites and users:

| Ref | What | Coupling |
| --- | --- | --- |
| `src/main/resources/mapper/ArticleReadService.xml:27-37` | `<sql id="selectArticleIds">` | `left join article_favorites AF on AF.article_id = A.id` (`:34`) and `left join users AFU on AFU.id = AF.user_id` (`:36`). |
| `src/main/resources/mapper/ArticleReadService.xml:47-62` | `queryArticles` (offset paging) | Filter `AND AFU.username = #{favoritedBy}` (`:56-58`). |
| `src/main/resources/mapper/ArticleReadService.xml:63-84` | `countArticle` | Duplicates the same joins inline (`:70`, `:72`) and filter (`:80-82`). |
| `src/main/resources/mapper/ArticleReadService.xml:107-133` | `findArticlesWithCursor` (cursor paging, GraphQL) | Includes `selectArticleIds`; filter at `:116-118`. |
| `src/main/java/io/spring/infrastructure/mybatis/readservice/ArticleReadService.java:16-25,37-41` | `queryArticles`, `countArticle`, `findArticlesWithCursor` | `@Param("favoritedBy")` on all three. |

## 4. Application layer

| Ref | What | Coupling |
| --- | --- | --- |
| `src/main/java/io/spring/application/data/ArticleData.java:20-21` | `favorited`, `favoritesCount` on the article read model | Serialized by Jackson into every REST article response; copied into the GraphQL `Article` type. |
| `src/main/java/io/spring/application/ArticleQueryService.java:28` | Injects `ArticleFavoritesReadService` | Application service depends directly on a MyBatis `@Mapper`. |
| `ArticleQueryService.java:42-52` (`findBySlug`) and `:30-40` (`findById`) | Single-article reads | Favorites only filled when `user != null` (`:35`, `:47`) via `fillExtraInfo(String, User, ArticleData)` (`:175-183`). **Anonymous single-article reads always report `favoritesCount = 0`**, while list reads (`:125-131`) always fill the count. Existing quirk – an extraction must decide whether to preserve it. |
| `ArticleQueryService.java:54-78`, `:100-111` | `findRecentArticlesWithCursor`, `findRecentArticles` | Pass `favoritedBy` (a username) into `ArticleReadService`. |
| `ArticleQueryService.java:80-98`, `:113-123` | Feed queries | Decorated with favorites via `fillExtraInfo(List, User)`. |
| `ArticleQueryService.java:148-159` | `setFavoriteCount` | `countMap.get(id)` is unboxed into `int` (`:158`) – a missing id from the favorites query would NPE. |
| `ArticleQueryService.java:161-173` | `setIsFavorite` | Passes the whole `User` to `userFavorites`. |

Other callers of `ArticleQueryService` that therefore transitively return favorite data:
`ArticleApi.java:38-41,58-60`, `ArticlesApi.java:35,45,56-58`, `ArticleDatafetcher.java:58,63,108,113,155,163,209,217,264,272,306-308,327-329,346`,
and `DuplicatedArticleValidator.java:16` (passes `user = null`, so it does not hit favorites queries for single reads).

