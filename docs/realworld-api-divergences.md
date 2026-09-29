# REST API vs. the RealWorld API spec

This document compares the REST surface of this application, as described in
[`openapi.yaml`](openapi.yaml), with the official RealWorld contract:

* OpenAPI: `specs/api/openapi.yml` in https://github.com/realworld-apps/realworld (OpenAPI 3.1.0, `info.version` 2.0.0)
* Behavioural suite: `specs/api/hurl/*.hurl` in the same repository (the upstream source of truth for API behaviour)

Both were taken from the upstream `main` branch on 2026-09-29.

## How this was produced

1. `docs/openapi.yaml` was written by hand from the `@RestController` classes in
   `io.spring.api`, `WebSecurityConfig`, `JwtTokenFilter`,
   `CustomizeExceptionHandler`, the request params and the `*Data` DTOs.
   Every status code and body shape in it was then checked with `curl`
   against the running application.
2. `OpenApiDocumentTest` fails if the set of `METHOD path` pairs in
   `docs/openapi.yaml` stops matching the Spring MVC handler mappings in
   `io.spring.api`, or if an operation references a divergence ID that is not
   listed in the table below.
3. The official Hurl suite (Hurl 8.0.1) was run against
   `./gradlew bootRun --args='--server.servlet.context-path=/api'` (see D01),
   twice:
   * **Unmodified suite:** 113 requests executed, **0/13 files pass**. Most
     failures cascade: `POST /articles` returns `200` instead of `201` (D08) and
     returns `500` when `tagList` is omitted (D15). Hurl skips captures when a
     status assertion fails, so the `slug` captures are never set and every
     later request in the file fails.
   * **Unmasked run:** a local copy of the suite where the 14 `POST /articles`
     requests with no `tagList` got `"tagList": []` added, and the 13
     `HTTP 201` assertions on `POST /articles` were relaxed to `HTTP *`. This
     makes the setup steps succeed so the later assertions actually run:
     153 requests executed, **2/13 files pass** (`pagination.hurl`,
     `tags.hurl`). Every other remaining failure is covered by an entry below.

   The upstream spec and suite were not modified.

## Assumptions

* **Scope is REST only.** The GraphQL API (`/graphql`, `/graphiql`) is not
  described. Its existence is noted as D02.
* **The comparison uses the application's default configuration.** The only
  exception is the Hurl runs, which added the `/api` context path so that the
  suite could reach the application at all.
* **The Hurl suite counts as part of the RealWorld spec.** Upstream treats it as
  the source of truth. Where the Hurl suite is stricter than `openapi.yml`
  (for example, `body` must be absent from list items, `tagList` must keep
  submission order, the NIST password rules), the Hurl behaviour is used as
  the expected behaviour.
* **`docs/openapi.yaml` describes current behaviour, including bugs.** For
  example, the omitted-`tagList` 500 is documented there as a response
  instead of being hidden.
* **Severity** is my own rating of client impact:
  * **High:** breaks a conforming client or the official suite.
  * **Medium:** wrong shape or status that a client could work around.
  * **Low:** an extra field, or a gap in the upstream spec.

## Divergences

"Official" means `openapi.yml` and the Hurl suite together. "Evidence" gives
the Hurl assertion that fails (file:line) or the manual probe that was used.

| ID | Sev | Area | Endpoints | Official | This implementation | Evidence |
|----|-----|------|-----------|----------|---------------------|----------|
| D01 | High | Routing | all | Server URL is `…/api`, so every route is under `/api` (`/api/users`, …) | No servlet context path is configured, so routes are served from the root (`/users`, `/articles`, …) | `application.properties`; the Hurl suite returns 404 for every request unless the app is started with `--server.servlet.context-path=/api` |
| D02 | Low | Routing | `/graphql`, `/graphiql` | Not part of the spec | A GraphQL API is also exposed and open to anonymous callers (`permitAll`) | `WebSecurityConfig`; `POST /graphql` → 200 |
| D03 | Medium | Errors | every operation with a body or numeric query params | Unprocessable input → `422` with `GenericErrorModel` | Missing or unparseable JSON, a body not wrapped in its root key (`{"user":{…}}`), or a non-integer `limit`/`offset` → `400` with an empty body | Probe: unwrapped login body → 400 empty; `?limit=abc` → 400 empty |
| D04 | Low | Auth | all authenticated operations | Header format is `Authorization: Token <jwt>` | The header is split on a space and the second segment is used, so any scheme word is accepted (`Bearer <jwt>`, `Foo <jwt>`) | `JwtTokenFilter.getTokenString`; probe with `Bearer` → 200 |
| D05 | High | Auth / Errors | all authenticated operations | Missing or invalid token → `401` with `{"errors":{"token":["is missing"]}}` | `401` with an **empty body** (Spring Security `HttpStatusEntryPoint`) | `errors_auth.hurl:119,130`, `errors_profiles.hurl`, `errors_articles.hurl`, `errors_comments.hurl`, `errors_authorization.hurl` (no-auth cases fail with "Invalid JSON") |
| D06 | Low | Auth | `GET /profiles/{username}`, `GET /articles`, `GET /articles/{slug}`, `GET /articles/{slug}/comments` | `401` is listed as a possible response | An invalid or expired token on a public GET is ignored and the request is served anonymously (`200`, `following`/`favorited` = `false`). These routes never return 401 | Probe: `GET /articles` with `Authorization: Token garbage` → 200 |
| D07 | High | Status + Errors | `POST /users/login` | Wrong credentials → `401` with `{"errors":{"credentials":["invalid"]}}` | `422` with `{"message":"invalid email or password"}`, which is not the `errors` envelope | `errors_auth.hurl:111,113`; `CustomizeExceptionHandler.handleInvalidAuthentication` |
| D08 | High | Status | `POST /articles` | `201 Created` | `200 OK` (`ResponseEntity.ok`) | Every `POST /articles` step in the suite, e.g. `articles.hurl:25` |
| D09 | High | Status + Errors | `POST /users` | Duplicate email or username → `409` with `{"errors":{"username":["has already been taken"]}}` | `422` with `{"errors":{"email":["duplicated email"]}}` / `{"errors":{"username":["duplicated username"]}}` | `errors_auth.hurl:62,75`; `DuplicatedEmailValidator`, `DuplicatedUsernameValidator` |
| D10 | High | Validation | `POST /articles` | A duplicate title is allowed and gets a different, unique slug (Hurl). `openapi.yml` also lists `409` | Rejected with `422` `{"errors":{"title":["article name exists"]}}` (`@DuplicatedArticleConstraint`), because slugs come from the title with no uniquifier | `errors_articles.hurl:139-143`; probe |
| D11 | High | Errors | every operation that takes `{slug}`, `{username}` or a comment `{id}` | `404` with `{"errors":{"article":["not found"]}}` (or `profile` / `comment`) | `404` with Spring Boot's default body `{"timestamp","status":404,"error":"Not Found","path"}` (`@ResponseStatus` on `ResourceNotFoundException`) | `errors_articles.hurl:19,155-188`, `errors_comments.hurl:67-87`, `errors_profiles.hurl:5,37,44`, `articles.hurl:256` |
| D12 | High | Errors | `PUT`/`DELETE /articles/{slug}`, `DELETE /articles/{slug}/comments/{id}` | `403` with `{"errors":{"article":["forbidden"]}}` (or `comment`) | `403` with Spring Boot's default body (`@ResponseStatus` on `NoAuthorizationException`) | `errors_authorization.hurl:47,59,78` |
| D13 | Medium | Errors | `POST /users`, `POST /users/login`, `PUT /user`, `POST /articles`, `POST …/comments` | Blank required field → `"can't be blank"` | `"can't be empty"`. Bad email format → `"should be an email"`, which the spec does not define | `errors_auth.hurl:12,25,38,89,101`, `errors_articles.hurl:82,97,112`, `errors_comments.hurl:55` |
| D14 | High | Validation | `PUT /user` | `email`/`username`/`password` set to `""` or `null` → `422`; `password` must be at least 8 characters (NIST 800-63B rules in Hurl); `bio`/`image` may be set to `null` | Missing, `null` and `""` all mean "leave unchanged", so these requests return `200` with nothing changed, `bio`/`image` can never be cleared, and there is no password length rule | `errors_auth.hurl:140-204` (7 cases return 200); `UpdateUserParam` defaults and `User.update` |
| D15 | High | Runtime defect | `POST /articles` | `tagList` is optional | Omitting `tagList` causes `NullPointerException` → `500` (`new HashSet<>(null)` in `Article`), so `tagList` is effectively required | Unmodified suite: 14 article-setup steps fail; probe |
| D16 | High | Request schema | `PUT /articles/{slug}` | `UpdateArticle.tagList` replaces the tags; `[]` removes all tags; `null` → `422` | `UpdateArticleParam` has no `tagList`. It is silently ignored: tags cannot be changed or cleared, and `null` returns `200` | `articles.hurl:228,235,245` |
| D17 | Medium | Response data | `PUT /articles/{slug}` | `updatedAt` changes when the article is updated | The `ArticleMapper.update` SQL never writes `updated_at`, so `updatedAt` always equals `createdAt` | `articles.hurl:179` |
| D18 | Low | Parameters | `GET /articles`, `GET /articles/feed` | `offset` minimum 0, `limit` minimum 1 (default 20); no maximum | Out-of-range values are not rejected: `limit <= 0` falls back to 20, `limit > 100` is capped at 100, and a negative `offset` becomes 0 | `Page`; probe `?limit=0`, `?limit=-1`, `?limit=1000` → 200 |
| D19 | High | Runtime defect | `GET /articles/feed` | `200` with an empty list when there are no articles | `500` when the caller follows at least one user but none of them has an article: `articlesFavoriteCount` is called with an empty id list, producing invalid SQL (`… where id in group by …`) | Probe; `bootrun.log` shows `SQLiteException near "group"`; this also broke `feed.hurl` in the unmodified run |
| D20 | Medium | Response schema | every operation that returns `user` or `profile` (including `author`) | `bio` and `image` are nullable and are `null` for a new user | Never `null`: `bio` defaults to `""` and `image` to `https://static.productionready.io/images/smiley-cyrus.jpg` | `auth.hurl`, `profiles.hurl` (`$.user.bio == null`, `$.user.image == null`, `$.profile.bio == null` …) |
| D21 | Low | Response schema | every operation that returns `article`/`articles` | `Article` has no `id` or `cursor` | Also includes `id` (UUID) and `cursor: {"data": <updatedAt>}`, which leak from the GraphQL `Node` interface | Probe; `ArticleData` |
| D22 | Medium | Response schema | `GET /articles`, `GET /articles/feed` | List items do **not** contain `body` (`MultipleArticlesResponse`; Hurl asserts `body not exists`) | List items are full `ArticleData`, including `body` | `articles.hurl:55,73,92,111,129`, `favorites.hurl:71,90`, `feed.hurl:84,102` |
| D23 | Medium | Response data | every operation that returns `article`/`articles` | `tagList` keeps submission order | `tagList` is returned in alphabetical order (the mapper result is sorted by tag name) | `articles.hurl:33-34`; probe (`["b","a"]` → `["a","b"]`) |
| D24 | Medium | Schema | `GET`/`POST /articles/{slug}/comments`, `DELETE /articles/{slug}/comments/{id}` | `Comment.id` and the `{id}` path parameter are `integer` | UUID `string` | `comments.hurl:39,66` |
| D25 | Low | Response schema | `GET`/`POST /articles/{slug}/comments` | `Comment` has no `cursor` | Also includes `cursor: {"data": <createdAt>}` | Probe; `CommentData` |
| D26 | Low | Status | `DELETE /profiles/{username}/follow` | Returns `200` with the profile (not following afterwards); a caller who is not following is not a documented error | `404` (Spring default body) if the caller is not currently following the user | `ProfileApi.unfollow`; not covered by Hurl |
| D27 | Low | Authorization | `DELETE /articles/{slug}/comments/{id}` | Deleting a comment is only described for its author (the Hurl non-author case expects `403`) | Also allowed for the **article** author (`AuthorizationService.canWriteComment`). This extends the spec rather than contradicting a Hurl case | Code; `errors_authorization.hurl` uses a third user and gets `403` |

## Areas that conform

* All 19 method + path pairs in `openapi.yml` exist, with the same path
  templates and HTTP methods. The implementation has no extra REST routes.
* Request and response bodies use the same root-object wrapping (`user`,
  `profile`, `article`, `articles` + `articlesCount`, `comment`, `comments`,
  `tags`).
* `201` for registration and for creating a comment; `204` for deleting an
  article or a comment.
* The `tag`, `author` and `favorited` filters, `offset`/`limit` pagination,
  and `createdAt`-descending ordering pass `pagination.hurl` and the filter
  assertions in `favorites.hurl`.
* Following a user, favoriting and unfavoriting an article, and
  `GET /tags` behave as specified, apart from the response-shape differences
  above (`tags.hurl` passes).
* Timestamps are ISO-8601 UTC with milliseconds
  (`2026-09-29T15:48:52.028Z`), which matches `format: date-time`.

## Reproducing

```sh
./gradlew bootRun --args='--server.servlet.context-path=/api'
git clone --depth 1 https://github.com/realworld-apps/realworld.git /tmp/realworld
cd /tmp/realworld/specs/api/hurl
hurl --test --jobs 1 --continue-on-error \
  --variable host=http://localhost:8080 --variable uid=r$(date +%s) *.hurl
```

Hurl 8.x is required, because the suite uses predicates such as `isList` that
Hurl 6 cannot parse.
