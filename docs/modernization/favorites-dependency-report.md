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

## 5. REST API

| Endpoint | Ref | Coupling |
| --- | --- | --- |
| `POST /articles/{slug}/favorite` | `src/main/java/io/spring/api/ArticleFavoriteApi.java:29-37` | Resolves slug via `ArticleRepository.findBySlug` (articles), user via `@AuthenticationPrincipal User` (users), writes `ArticleFavorite`, responds with `ArticleQueryService.findBySlug(...).get()`. |
| `DELETE /articles/{slug}/favorite` | `ArticleFavoriteApi.java:39-51` | Same dependencies; no-op if not favorited. |
| `GET /articles?favorited={username}` | `src/main/java/io/spring/api/ArticlesApi.java:48-59` (param at `:53`) | Query param is named `favorited` but bound to `favoritedBy`; value is a **username**, not an id. |
| `GET /articles`, `GET /articles/feed`, `GET/PUT /articles/{slug}`, `POST /articles` | `ArticlesApi.java:28-59`, `ArticleApi.java:35-63` | Response bodies include `favorited` / `favoritesCount` from `ArticleData`. |
| Security | `src/main/java/io/spring/api/security/WebSecurityConfig.java:59-62` | Favorite POST/DELETE fall through to `anyRequest().authenticated()`; `GET /articles/**` is public. |

## 6. GraphQL API (Netflix DGS)

| Element | Ref | Coupling |
| --- | --- | --- |
| Schema – mutations | `src/main/resources/schema/schema.graphqls:32-33` | `favoriteArticle(slug)`, `unfavoriteArticle(slug)` return `ArticlePayload`. |
| Schema – fields | `schema.graphqls:53-54` | `Article.favorited`, `Article.favoritesCount` (both non-null). |
| Schema – filters | `schema.graphqls:10`, `:109` | `Query.articles(favoritedBy:)`, `Profile.favorites(...)` connection. |
| Codegen | `build.gradle:5`, `:69-72` | `DgsConstants.MUTATION.FavoriteArticle/UnfavoriteArticle`, `PROFILE.Favorites` and `types.Article` are generated from the schema; renaming schema fields breaks Java compilation. |
| Mutations | `src/main/java/io/spring/graphql/ArticleMutation.java:32` (field), `:72-83`, `:85-100` | Same pattern as REST: `SecurityUtil.getCurrentUser()` (users) + `ArticleRepository.findBySlug` (articles) + `ArticleFavoriteRepository`. Returns empty `ArticlePayload` with the core `Article` in `localContext`. |
| Payload resolution | `src/main/java/io/spring/graphql/ArticleDatafetcher.java:300-319` | `ArticlePayload.article` re-reads via `ArticleQueryService.findById` to get fresh `favorited`/`favoritesCount`. |
| `Profile.favorites` | `ArticleDatafetcher.java:138-190` | Uses `profile.getUsername()` (`:158`, `:166`) as `favoritedBy`. |
| `Query.articles(favoritedBy)` | `ArticleDatafetcher.java:245-298` (arg `:252`, use `:267`, `:275`) | Pass-through to `ArticleQueryService`. |
| Mapping to GraphQL type | `ArticleDatafetcher.java:371-383` (`:376-377`) | Copies `favorited` / `favoritesCount`. |
| Security | `WebSecurityConfig.java:53-54` | `/graphql` is `permitAll`; auth enforced inside the mutation via `AuthenticationException`. |

## 7. Tests

| Test | Ref | What it covers |
| --- | --- | --- |
| `ArticleFavoriteApiTest` | `src/test/java/io/spring/api/ArticleFavoriteApiTest.java:33-103` | `@WebMvcTest` of REST favorite/unfavorite with mocked `ArticleFavoriteRepository`, `ArticleRepository`, `ArticleQueryService`; asserts `save` (`:86`) and `remove` with an equal `ArticleFavorite` (`:101`). |
| `MyBatisArticleFavoriteRepositoryTest` | `src/test/java/io/spring/infrastructure/favorite/MyBatisArticleFavoriteRepositoryTest.java:12-34` | Real DB: save/find (`:20-25`) and remove (`:28-33`). Uses the mapper directly (`:17`). |
| `ArticleQueryServiceTest` | `src/test/java/io/spring/application/article/ArticleQueryServiceTest.java` | Wires `MyBatisArticleFavoriteRepository` (`:35`, `:44`); favorite count/flag on single read (`:65-66`, `:73-84`); `favoritedBy` filter (`:161-181`). |
| `ArticlesApiTest` | `src/test/java/io/spring/api/ArticlesApiTest.java:88-90` | Asserts `article.favorited == false`, `article.favoritesCount == 0` in create-article response. |
| `ArticleApiTest`, `ListArticleApiTest` | `src/test/java/io/spring/api/ArticleApiTest.java`, `ListArticleApiTest.java` | Indirect: mock `ArticleQueryService` and build `ArticleData` instances with the `favorited`/`favoritesCount` constructor args. |

**Gaps:** no tests for `ArticleMutation.favoriteArticle` / `unfavoriteArticle`, `Profile.favorites`,
`Query.articles(favoritedBy:)`, the REST `?favorited=` query param end-to-end, or orphan rows after article deletion.

## 8. Coupling summary

```
            users (id, username)                     articles (id, slug, ...)
                 ^       ^                                  ^        ^
   AFU.username  |       | User.id               A.id joins |        | ArticleRepository.findBySlug
  (ArticleRead-  |       | (@AuthenticationPrincipal,       |        | (ArticleFavoriteApi, ArticleMutation)
   Service.xml)  |       |  SecurityUtil, userFavorites)    |        |
                 +----- article_favorites (article_id, user_id) -----+
                                   ^
        ArticleFavoriteMapper(.xml) / ArticleFavoritesReadService(.xml) / TransferData.xml
                                   ^
                 ArticleQueryService -> ArticleData.favorited / favoritesCount
                                   ^
        REST: ArticleFavoriteApi, ArticlesApi, ArticleApi    GraphQL: ArticleMutation, ArticleDatafetcher, schema.graphqls
```

## 9. What would break if we extracted this

Assuming "extract" means moving favorites into its own module/service with its own storage, so that
`article_favorites` is no longer joinable from the articles queries:

1. **Article list filtering by favoriter** – `queryArticles`, `countArticle` and `findArticlesWithCursor`
   (`ArticleReadService.xml:34,36,56-58,70,72,80-82,116-118`) would fail at SQL level. Affects
   `GET /articles?favorited=`, GraphQL `articles(favoritedBy:)` and `Profile.favorites`. These need to become
   "ask favorites for article ids favorited by user X, then fetch articles by id" – which also changes how
   offset/cursor pagination and the total `articlesCount` are computed.
2. **Favorite counts / flags on every article response** – `articlesFavoriteCount` and `userFavorites`
   (`ArticleFavoritesReadService.xml:10-29`) join `articles`; `ArticleQueryService` (`:28`, `:148-177`) calls the
   mapper in-process. Every REST and GraphQL article payload would lose `favorited`/`favoritesCount` (GraphQL
   declares them non-null, so queries would error) unless replaced by a client call or a batch API.
3. **Username vs. id** – the filter is by `users.username` (`AFU.username`). An extracted service that only
   stores ids needs a username→id lookup from users first.
4. **Domain type leakage** – `ArticleFavoritesReadService.userFavorites` takes `io.spring.core.user.User`
   (`:18`, XML `:28`); `ArticleFavoriteMapper.xml:17` and `TransferData.xml:4` hard-code class FQNs. Moving packages
   breaks these at runtime (MyBatis XML is not checked by the compiler) – only DB tests would catch it.
5. **Write endpoints** – `ArticleFavoriteApi` and `ArticleMutation` resolve slugs through `ArticleRepository` and
   re-read through `ArticleQueryService`; an extracted favorites service needs an article lookup (slug→id) and the
   APIs need to keep returning the full article payload to stay contract-compatible.
6. **Referential integrity** – there are no FKs today and `ArticleMapper.xml:32-34` deletes articles without
   touching favorites, so orphan rows can already occur; `isUserFavorite` / `articleFavoriteCount` don't join `articles`
   and would still count them. An extraction must define delete-propagation (event, cascade or tolerated orphans).
7. **Transactions** – favoriting and reading the updated article happen in one request/DB; after extraction the
   immediate re-read (`ArticleFavoriteApi.java:36,50`, `ArticleDatafetcher.java:306-308`) may be eventually consistent.
8. **Tests** – `ArticleQueryServiceTest` (`:35`, `:44`, `:73-84`, `:161-181`) seeds favorites through
   `MyBatisArticleFavoriteRepository` in the same DB and would need a fake/stub; `ArticleFavoriteApiTest` would need
   to mock the new client instead of the repository. GraphQL paths are untested, so regressions there would go unnoticed
   – adding tests before extraction is recommended.
9. **Behavioural quirk to preserve or fix deliberately** – anonymous single-article reads return
   `favoritesCount = 0` (`ArticleQueryService.java:35,47`), while list reads return the real count.
