# API reference

The backend exposes the same domain through a REST API (`src/main/java/io/spring/api`) and a GraphQL API (`src/main/java/io/spring/graphql`). Both run on port 8080 under the root path.

## Authentication

- Get a token from `POST /users`, `POST /users/login`, or the GraphQL `createUser`/`login` mutations (`user { token }`).
- Send it as `Authorization: Token <jwt>`. `JwtTokenFilter` takes the second whitespace-separated part of the header, so the prefix word itself isn't checked.
- Tokens are HS512-signed JWTs. The subject (`sub`) is the user id, and they expire after `jwt.sessionTime` seconds (default 86400).
- A missing or invalid token on a protected REST route returns `401` with an empty body.

## REST

All request bodies are wrapped in a root object (`spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true`), as shown below.

### Users

| Method & path | Auth | Request body | Success response |
| --- | --- | --- | --- |
| `POST /users` | - | `{"user": {"email", "username", "password"}}` | `201` `{"user": User}` |
| `POST /users/login` | - | `{"user": {"email", "password"}}` | `200` `{"user": User}` |
| `GET /user` | required | - | `200` `{"user": User}` (echoes the request token) |
| `PUT /user` | required | `{"user": {"email"?, "username"?, "password"?, "bio"?, "image"?}}` | `200` `{"user": User}` |

`User` = `{"email", "username", "bio", "image", "token"}`.

Validation: on register, `email` (valid format, unique), `username` (unique) and `password` must not be blank. On update, a non-empty `email` must be valid, and `email`/`username` must not belong to another user. Empty or omitted fields leave the stored value unchanged.

### Profiles

| Method & path | Auth | Success response |
| --- | --- | --- |
| `GET /profiles/{username}` | optional | `200` `{"profile": Profile}` |
| `POST /profiles/{username}/follow` | required | `200` `{"profile": Profile}` (following an already-followed user does nothing) |
| `DELETE /profiles/{username}/follow` | required | `200` `{"profile": Profile}`. Returns `404` if you don't follow the user |

`Profile` = `{"username", "bio", "image", "following"}`.

### Articles

| Method & path | Auth | Request | Success response |
| --- | --- | --- | --- |
| `GET /articles` | optional | query: `tag`, `author`, `favorited` (username), `offset` (default 0), `limit` (default 20, max 100) | `200` `{"articles": [Article], "articlesCount": n}` |
| `GET /articles/feed` | required | query: `offset`, `limit` | `200` `{"articles": [Article], "articlesCount": n}`: articles by users you follow |
| `POST /articles` | required | `{"article": {"title", "description", "body", "tagList"?}}` | `200` `{"article": Article}` |
| `GET /articles/{slug}` | optional | - | `200` `{"article": Article}` |
| `PUT /articles/{slug}` | author only | `{"article": {"title"?, "description"?, "body"?}}` | `200` `{"article": Article}` |
| `DELETE /articles/{slug}` | author only | - | `204` |
| `POST /articles/{slug}/favorite` | required | - | `200` `{"article": Article}` |
| `DELETE /articles/{slug}/favorite` | required | - | `200` `{"article": Article}` |

`Article` = `{"id", "slug", "title", "description", "body", "favorited", "favoritesCount", "createdAt", "updatedAt", "tagList", "author": Profile}`. Timestamps are ISO-8601 in UTC. Responses also include an internal `"cursor"` field holding the article's `updatedAt`, which leaks from `ArticleData.getCursor()` (the RealWorld spec doesn't include it).

Notes:
- Lists are ordered by `created_at` descending.
- The slug is derived from the title (`Article.toSlug`: lower-cased, with whitespace and some punctuation replaced by `-`). Creating an article whose slug already exists fails validation with `title: "article name exists"`.
- Changing the title on update regenerates the slug. Tags can't be changed after creation.

### Comments

| Method & path | Auth | Request | Success response |
| --- | --- | --- | --- |
| `GET /articles/{slug}/comments` | optional | - | `200` `{"comments": [Comment]}` (all comments, no pagination) |
| `POST /articles/{slug}/comments` | required | `{"comment": {"body"}}` | `201` `{"comment": Comment}` |
| `DELETE /articles/{slug}/comments/{id}` | comment author or article author | - | `204` |

`Comment` = `{"id", "body", "createdAt", "updatedAt", "author": Profile}`.

### Tags

| Method & path | Auth | Success response |
| --- | --- | --- |
| `GET /tags` | - | `200` `{"tags": [string]}` |

### Errors

| Situation | Status | Body |
| --- | --- | --- |
| Bean-validation failure | `422` | `{"errors": {"<field>": ["<message>", ...]}}` |
| Wrong email/password on login | `422` | `{"message": "invalid email or password"}` |
| Missing/invalid token on a protected route | `401` | empty |
| Authenticated but not the owner | `403` | Spring Boot default error JSON (`timestamp`, `status`, `error`, `path`) |
| Unknown slug/username/comment | `404` | Spring Boot default error JSON |

Exception mapping lives in `api/exception/CustomizeExceptionHandler.java`.

## GraphQL

- Endpoint: `POST /graphql` (permitted without auth at the HTTP layer, so each resolver checks authentication itself).
- Schema: `src/main/resources/schema/schema.graphqls`. The `generateJava` Gradle task (DGS codegen) generates the types into `io.spring.graphql` / `io.spring.graphql.types`.
- Send the same `Authorization: Token <jwt>` header for authenticated operations.

### Queries

| Field | Auth | Notes |
| --- | --- | --- |
| `article(slug)` | optional | An unknown slug returns an `INTERNAL` error wrapping `ResourceNotFoundException` |
| `articles(first, after, last, before, authoredBy, favoritedBy, withTag)` | optional | Cursor connection |
| `feed(first, after, last, before)` | required in practice | Articles by followed users. Calling it anonymously currently fails with a `NullPointerException` error instead of an authentication error |
| `me` | required in practice | The resolver declares a required `Authorization` header, so an anonymous call returns an `INTERNAL` error ("Required header 'Authorization' was not provided") |
| `profile(username)` | optional | `ProfilePayload` |
| `tags` | - | `[String]` |

Nested connections: `Article.comments`, `Profile.articles`, `Profile.favorites`, `Profile.feed`.

### Mutations

| Field | Auth | Returns |
| --- | --- | --- |
| `createUser(input: {email, username, password})` | - | `UserResult` union: `UserPayload`, or `Error {message: "BAD_REQUEST", errors: [{key, value}]}` on validation failure (e.g. `duplicated email`) |
| `login(email, password)` | - | `UserPayload`. Bad credentials produce an `UNAUTHENTICATED` error |
| `updateUser(changes: {email, username, password, image, bio})` | required | `UserPayload`. Returns `null`, with no error, when anonymous |
| `followUser(username)` / `unfollowUser(username)` | required | `ProfilePayload` |
| `createArticle(input: {title, description, body, tagList})` | required | `ArticlePayload` |
| `updateArticle(slug, changes: {title, description, body})` | author only | `ArticlePayload` |
| `favoriteArticle(slug)` / `unfavoriteArticle(slug)` | required | `ArticlePayload` |
| `deleteArticle(slug)` | author only | `DeletionStatus {success}` |
| `addComment(slug, body)` | required | `CommentPayload` |
| `deleteComment(slug, id)` | comment or article author | `DeletionStatus {success}` |

### Pagination

Connections follow the Relay shape (`edges { cursor node }`, `pageInfo { hasNextPage hasPreviousPage startCursor endCursor }`).

- Pass `first` (with an optional `after`) to page forward, or `last` (with an optional `before`) to page backward. If you omit both, the resolver throws `IllegalArgumentException` (its message is in Chinese: "first 和 last 必须只存在一个").
- A cursor is a timestamp in epoch milliseconds (`DateTimeCursor`). Comment cursors use `createdAt` (`CommentData.getCursor`). Article cursors use `updatedAt` (`ArticleData.getCursor`), but the article queries filter and sort on `created_at`, so paging through edited articles can skip or repeat items.
- When paging forward only `hasNextPage` is computed, and when paging backward only `hasPreviousPage` is computed. The other flag is always `false`.

### Errors

`graphql/exception/GraphQLCustomizeExceptionHandler.java` maps:
- `InvalidAuthenticationException` to an `UNAUTHENTICATED` error
- `ConstraintViolationException` to a `BAD_REQUEST` error with field messages in `extensions`
- everything else to DGS's default handler. In practice the REST exceptions reused by the resolvers (`ResourceNotFoundException`, `NoAuthorizationException`) and the GraphQL `AuthenticationException` all come back as `errorType: INTERNAL` with the exception class name in `message`

### Example

```bash
# register (createUser returns the UserResult union: UserPayload or Error)
curl -s localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createUser(input: {email: \"me@example.com\", username: \"me\", password: \"secret\"}) { ... on UserPayload { user { username } } ... on Error { message errors { key value } } } }"}'

TOKEN=$(curl -s localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"mutation { login(email: \"me@example.com\", password: \"secret\") { user { token } } }"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -s localhost:8080/graphql -H 'Content-Type: application/json' -H "Authorization: Token $TOKEN" \
  -d '{"query":"{ me { username } feed(first: 10) { edges { node { slug title } } pageInfo { hasNextPage endCursor } } }"}'
```
