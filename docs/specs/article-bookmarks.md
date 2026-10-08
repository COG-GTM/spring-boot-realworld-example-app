# Spec: Article Bookmarks (Reading List)

| Field   | Value                                   |
|---------|-----------------------------------------|
| Status  | Accepted (implemented alongside this spec) |
| Owner   | Backend                                 |
| Scope   | REST API, GraphQL API, persistence      |

## 1. Problem statement

Readers can *favorite* articles, but favorites are a public signal: they drive the
`favoritesCount` that every reader sees and they are queryable by anyone through
`GET /articles?favorited={username}` and `Profile.favorites`. Readers have no way to
privately save an article to read later without broadcasting an endorsement.

This feature adds **bookmarks**: a private, per-user reading list. Bookmarking is
independent of favoriting, is visible only to the bookmarking user, and never affects
public counters.

## 2. Goals / non-goals

**Goals**

- Authenticated users can bookmark and unbookmark any existing article, idempotently.
- Authenticated users can list their own bookmarks, most recently bookmarked first,
  with offset pagination (REST) and cursor pagination (GraphQL).
- Every article representation tells the authenticated user whether they bookmarked it.

**Non-goals**

- No public bookmark counts, no listing of another user's bookmarks.
- No folders/tags/notes on bookmarks.
- No change to favorites behavior.

## 3. User stories

- **US-1** As a signed-in reader, I want to bookmark an article so I can find it later.
- **US-2** As a signed-in reader, I want to remove a bookmark once I've read the article.
- **US-3** As a signed-in reader, I want to see my reading list, newest bookmark first,
  one page at a time.
- **US-4** As a signed-in reader browsing articles, I want to see at a glance which
  articles are already in my reading list.
- **US-5** As a reader, I want my reading list to stay private and not change any public
  numbers on the article.
- **US-6** As a GraphQL client developer, I want the same capability through the GraphQL
  schema so web and mobile clients have parity.

## 4. REST contract

> **Base path.** The RealWorld convention writes paths under `/api`. As documented in the
> README, this service is served from the application root (no `/api` context path), so
> the paths below are mapped exactly like the existing endpoints, e.g.
> `/articles/{slug}/favorite`. A deployment that mounts the service under `/api` exposes
> them as `/api/articles/{slug}/bookmark` and `/api/user/bookmarks`.

Authentication uses the existing header: `Authorization: Token <jwt>`.

### 4.1 `POST /articles/{slug}/bookmark` (`/api/articles/{slug}/bookmark`)

Bookmark an article for the current user.

- Request body: none.
- `200 OK` with the single-article envelope (same shape as `GET /articles/{slug}`), with
  `bookmarked: true`.
- Idempotent: bookmarking an already-bookmarked article returns `200` with the same body and
  does not create a second record.
- `401 Unauthorized` if no/invalid token. `404 Not Found` if `slug` does not exist.

```json
{
  "article": {
    "id": "8f0c...",
    "slug": "how-to-train-your-dragon",
    "title": "How to train your dragon",
    "description": "Ever wonder how?",
    "body": "It takes a Jacobian",
    "favorited": false,
    "favoritesCount": 0,
    "bookmarked": true,
    "createdAt": "2016-02-18T03:22:56.637Z",
    "updatedAt": "2016-02-18T03:48:35.824Z",
    "tagList": ["dragons", "training"],
    "author": {
      "id": "1b2c...",
      "username": "jake",
      "bio": "I work at statefarm",
      "image": "https://example.com/jake.jpg",
      "following": false
    }
  }
}
```

### 4.2 `DELETE /articles/{slug}/bookmark` (`/api/articles/{slug}/bookmark`)

Remove the current user's bookmark.

- Request body: none.
- `200 OK` with the single-article envelope and `bookmarked: false`.
- Idempotent: removing a bookmark that does not exist returns `200` with `bookmarked: false`.
- `401 Unauthorized` if no/invalid token. `404 Not Found` if `slug` does not exist.

### 4.3 `GET /user/bookmarks?limit={n}&offset={m}` (`/api/user/bookmarks`)

List the current user's bookmarked articles.

- Query params (same semantics as `GET /articles` via `io.spring.application.Page`):
  - `limit` - default `20`; values `<= 0` fall back to `20`; values `> 100` are capped at `100`.
  - `offset` - default `0`; negative values fall back to `0`.
- Ordering: most recently bookmarked first (bookmark `created_at` descending).
- `200 OK` with the multiple-articles envelope (same shape as `GET /articles`). Every
  returned article has `bookmarked: true`. `articlesCount` is the total number of the user's
  bookmarks (ignoring `limit`/`offset`).
- Bookmarks whose article has been deleted are neither returned nor counted.
- `401 Unauthorized` if no/invalid token.

```json
{
  "articles": [
    { "slug": "...", "title": "...", "bookmarked": true, "favorited": false, "...": "..." }
  ],
  "articlesCount": 1
}
```

### 4.4 `bookmarked` on all article responses

Every existing article representation (`GET /articles/{slug}`, `GET /articles`,
`GET /articles/feed`, create/update/favorite/unfavorite responses) gains a boolean
`bookmarked` field, placed next to `favorited`/`favoritesCount`:

- `true` iff the **authenticated** user has bookmarked the article.
- `false` for anonymous requests and for articles bookmarked only by other users.

## 5. GraphQL contract

Changes to `src/main/resources/schema/schema.graphqls` (additive only):

```graphql
type Mutation {
    # ...existing...
    bookmarkArticle(slug: String!): ArticlePayload
    unbookmarkArticle(slug: String!): ArticlePayload
}

type Article {
    # ...existing...
    bookmarked: Boolean!
}

### The viewer (returned by `me`)
type User {
    # ...existing...
    bookmarks(first: Int, after: String): ArticlesConnection
}
```

Semantics:

- `bookmarkArticle(slug)` / `unbookmarkArticle(slug)` behave like the REST endpoints and
  return `ArticlePayload { article }` with `article.bookmarked` reflecting the new state.
  Both are idempotent.
- `Article.bookmarked` follows §4.4.
- `User.bookmarks(first, after)` is the viewer's reading list (reached through `me`):
  - Ordered most recently bookmarked first.
  - `first` defaults to `20` when omitted; capped by the existing `CursorPageParameter`
    limit (1000).
  - Edge `cursor` is the bookmark creation time in epoch millis (same encoding as
    `DateTimeCursor` used elsewhere); `after` returns bookmarks created strictly before it.
  - `pageInfo.hasNextPage` is `true` when more bookmarks exist after the last edge;
    `pageInfo.endCursor` is the last edge's cursor.
- Errors:
  - Unauthenticated `bookmarkArticle` / `unbookmarkArticle` / `bookmarks`: the field
    resolves to `null` and an entry is added to `errors` (existing `AuthenticationException`
    path, as for `favoriteArticle`).
  - Unknown slug: the field resolves to `null` and an entry is added to `errors`
    (existing `ResourceNotFoundException` path, as for `favoriteArticle`).

## 6. Data model

New Flyway migration `src/main/resources/db/migration/V2__create_article_bookmarks.sql`:

```sql
create table article_bookmarks (
  article_id varchar(255) not null,
  user_id varchar(255) not null,
  created_at TIMESTAMP NOT NULL,
  constraint uk_article_bookmarks_user_article unique (user_id, article_id)
);
create index idx_article_bookmarks_user_created on article_bookmarks (user_id, created_at);
```

- One row per (user, article); the unique constraint enforces it at the database level.
- `created_at` is written by the application (`DateTimeHandler`, same storage format as
  `articles.created_at`) and drives ordering/cursors.
- Bookmark rows are not cascaded when an article is deleted (matching
  `article_favorites`); read queries inner-join `articles` so orphaned rows are invisible.

## 7. Design (layering, mirroring favorites)

| Layer | Favorites (existing) | Bookmarks (new) |
|-------|----------------------|-----------------|
| core domain | `core.favorite.ArticleFavorite`, `ArticleFavoriteRepository` | `core.bookmark.ArticleBookmark`, `ArticleBookmarkRepository` |
| infrastructure write | `MyBatisArticleFavoriteRepository`, `ArticleFavoriteMapper(.xml)` | `MyBatisArticleBookmarkRepository`, `ArticleBookmarkMapper(.xml)` |
| infrastructure read | `ArticleFavoritesReadService(.xml)` | `ArticleBookmarksReadService(.xml)` |
| application query | `ArticleQueryService` (favorited/favoritesCount) | `ArticleQueryService` (bookmarked, `findUserBookmarks`, `findUserBookmarksWithCursor`) |
| REST | `ArticleFavoriteApi` | `ArticleBookmarkApi`, `UserBookmarksApi` |
| GraphQL | `ArticleMutation.favoriteArticle` | `ArticleMutation.bookmarkArticle/unbookmarkArticle`, `ArticleDatafetcher` `User.bookmarks` |

## 8. Auth rules

| Operation | Anonymous | Authenticated, unknown slug | Authenticated, valid slug |
|-----------|-----------|-----------------------------|---------------------------|
| `POST /articles/{slug}/bookmark` | 401 | 404 | 200, idempotent |
| `DELETE /articles/{slug}/bookmark` | 401 | 404 | 200, idempotent |
| `GET /user/bookmarks` | 401 | n/a | 200 (own bookmarks only) |
| `bookmarkArticle` / `unbookmarkArticle` | error, `null` | error, `null` | payload, idempotent |
| `me { bookmarks }` | `me` is `null` | n/a | connection (own bookmarks only) |

A user can only ever read or modify **their own** bookmarks; no endpoint accepts a user id.

## 9. Acceptance criteria

- **AC-1** `POST /articles/{slug}/bookmark` by an authenticated user returns `200` with the
  article envelope where `article.bookmarked == true`, and persists a bookmark for
  (user, article).
- **AC-2** Bookmarking is idempotent: repeating `POST /articles/{slug}/bookmark` returns `200`
  and leaves exactly one bookmark row for (user, article).
- **AC-3** `DELETE /articles/{slug}/bookmark` returns `200` with `article.bookmarked == false`
  and removes the bookmark.
- **AC-4** Unbookmarking is idempotent: `DELETE` on an article that is not bookmarked returns
  `200` and performs no removal.
- **AC-5** `POST`/`DELETE /articles/{slug}/bookmark` and `GET /user/bookmarks` without a valid
  token return `401`.
- **AC-6** `POST`/`DELETE /articles/{slug}/bookmark` for an unknown slug return `404`.
- **AC-7** `GET /user/bookmarks` returns `{ articles, articlesCount }` containing only the
  current user's bookmarks, most recently bookmarked first, each with `bookmarked == true`.
- **AC-8** `GET /user/bookmarks` honors `limit`/`offset` (defaults `20`/`0`), and
  `articlesCount` is the total, independent of the page.
- **AC-9** Article responses include `bookmarked`: `true` iff the authenticated user bookmarked
  the article, `false` for other users and anonymous requests (single and list reads).
- **AC-10** Bookmarks are private and independent of favorites: bookmarking does not change
  `favorited`/`favoritesCount`, and one user's bookmarks do not affect another user's view.
- **AC-11** Flyway migration `V2__create_article_bookmarks.sql` creates `article_bookmarks`
  with a unique (user_id, article_id) constraint; a duplicate insert is rejected by the
  database.
- **AC-12** GraphQL `bookmarkArticle(slug)` returns `ArticlePayload` with
  `article.bookmarked == true`; calling it twice is idempotent.
- **AC-13** GraphQL `unbookmarkArticle(slug)` returns `ArticlePayload` with
  `article.bookmarked == false`; calling it when not bookmarked is idempotent.
- **AC-14** GraphQL `bookmarkArticle`/`unbookmarkArticle` return `null` plus an error when
  unauthenticated or when the slug is unknown.
- **AC-15** GraphQL `me { bookmarks(first, after) }` returns an `ArticlesConnection` of the
  viewer's bookmarks, newest first, whose `pageInfo.hasNextPage`/`endCursor` allow fetching
  the next page via `after`.
- **AC-16** Bookmarks pointing to deleted articles are excluded from listings and counts
  (REST and GraphQL).

## 10. Test plan

Each AC is covered by at least one test whose name/comment cites the AC id; see the PR
description for the AC -> test mapping. Layers: MockMvc/RestAssured API tests
(`ArticleBookmarkApiTest`, `UserBookmarksApiTest`), MyBatis repository/mapper tests
(`MyBatisArticleBookmarkRepositoryTest`), query-service tests against SQLite
(`ArticleBookmarkQueryServiceTest`), and end-to-end GraphQL tests through
`DgsQueryExecutor` (`ArticleBookmarkGraphQLTest`).
