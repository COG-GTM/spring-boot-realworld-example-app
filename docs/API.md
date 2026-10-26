# API reference

The backend exposes the same features through a REST API (RealWorld spec) and a GraphQL API. Base URL for both: `http://localhost:8080`.

- [Authentication](#authentication)
- [REST API](#rest-api)
- [Errors (REST)](#errors-rest)
- [GraphQL API](#graphql-api)

## Authentication

Obtain a token from `POST /users`, `POST /users/login`, or the GraphQL `createUser` / `login` mutations, then send:

```
Authorization: Token <jwt>
```

`JwtTokenFilter` reads the second whitespace-separated part of the `Authorization` header (so any scheme word works, e.g. `Token` or `Bearer`), validates it with the HS512 key from `jwt.secret`, and loads the user whose id is the token subject. Invalid or expired tokens are ignored, so the request continues as anonymous.

Route access is configured in `api/security/WebSecurityConfig.java`:

| Rule | Access |
| --- | --- |
| `OPTIONS` any path | public |
| `/graphql`, `/graphiql` | public (GraphQL resolvers check auth themselves) |
| `GET /articles/feed` | authenticated |
| `POST /users`, `POST /users/login` | public |
| `GET /articles/**`, `GET /profiles/**`, `GET /tags` | public (user-specific fields are filled when a valid token is sent) |
| anything else | authenticated (`401` otherwise) |

## REST API

Request bodies must be wrapped in a root key (`spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true`); responses are wrapped by the controllers. Timestamps are ISO-8601 in UTC, e.g. `2026-01-01T12:00:00.000Z`.

### Users

**`POST /users`** – register. Returns `201`.

```json
{"user": {"email": "jake@jake.jake", "username": "jake", "password": "jakejake"}}
```

All fields are required; `email` must be a valid, unused address and `username` must be unused. New users get an empty `bio` and the `image.default` avatar.

**`POST /users/login`** – log in.

```json
{"user": {"email": "jake@jake.jake", "password": "jakejake"}}
```

Both return:

```json
{"user": {"email": "jake@jake.jake", "username": "jake", "bio": "", "image": "https://...", "token": "<jwt>"}}
```

**`GET /user`** (auth) – current user, same shape as above. The `token` echoed back is the one sent in the request.

**`PUT /user`** (auth) – update the current user. Any subset of `email`, `username`, `password`, `bio`, `image`; empty or missing fields are left unchanged. Email/username must not belong to another user.

```json
{"user": {"bio": "I like to skateboard", "image": "https://i.stack.imgur.com/xHWG8.jpg"}}
```

### Profiles

**`GET /profiles/{username}`** – `{"profile": {"username", "bio", "image", "following"}}`. `following` is `false` for anonymous callers.

**`POST /profiles/{username}/follow`** / **`DELETE /profiles/{username}/follow`** (auth) – follow / unfollow; return the profile. Unfollowing a user you don't follow returns `404`.

### Articles

Article object:

```json
{
  "id": "…",
  "slug": "how-to-train-your-dragon",
  "title": "How to train your dragon",
  "description": "Ever wonder how?",
  "body": "You have to believe",
  "tagList": ["dragons", "training"],
  "createdAt": "…",
  "updatedAt": "…",
  "favorited": false,
  "favoritesCount": 0,
  "author": {"username": "jake", "bio": "", "image": "…", "following": false}
}
```

The serialized article also carries `id` and a `cursor` field (an artifact of the GraphQL pagination interface) that are not part of the RealWorld spec.

**`GET /articles`** – list most recent articles first. Query params: `tag`, `author` (username), `favorited` (username), `offset` (default `0`), `limit` (default `20`, capped at `100`). Response: `{"articles": [...], "articlesCount": N}` where `articlesCount` is the total matching count.

**`GET /articles/feed`** (auth) – articles authored by users the caller follows; `offset`, `limit`. Same response shape.

**`POST /articles`** (auth) – create.

```json
{"article": {"title": "How to train your dragon", "description": "Ever wonder how?", "body": "You have to believe", "tagList": ["dragons", "training"]}}
```

`title`, `description`, `body` are required; `tagList` is optional. The slug is derived from the title (lowercased, whitespace and some punctuation replaced by `-`); a title whose slug already exists is rejected with `422`. Returns `200` with `{"article": {...}}`.

**`GET /articles/{slug}`** – `{"article": {...}}` or `404`.

**`PUT /articles/{slug}`** (auth, author only) – any of `title`, `description`, `body`. Changing the title regenerates the slug. Non-authors get `403`.

**`DELETE /articles/{slug}`** (auth, author only) – `204`.

**`POST /articles/{slug}/favorite`** / **`DELETE /articles/{slug}/favorite`** (auth) – returns the updated article.

### Comments

**`GET /articles/{slug}/comments`** – `{"comments": [{"id", "body", "createdAt", "updatedAt", "author": {...}}]}`.

**`POST /articles/{slug}/comments`** (auth) – `{"comment": {"body": "Thank you so much!"}}`; returns `201` with `{"comment": {...}}`.

**`DELETE /articles/{slug}/comments/{id}`** (auth) – allowed for the comment author or the article author (`403` otherwise); returns `204`.

### Tags

**`GET /tags`** – `{"tags": ["dragons", "training"]}`.

## Errors (REST)

Handled in `api/exception/CustomizeExceptionHandler.java` and the exception classes next to it.

| Situation | Status | Body |
| --- | --- | --- |
| Bean validation failure (`@Valid` body, duplicate email/username/title) | `422` | `{"errors": {"email": ["duplicated email"], "username": ["can't be empty"]}}` |
| Wrong email or password on login | `422` | `{"message": "invalid email or password"}` |
| Missing / invalid token on a protected route | `401` | empty |
| Not the owner (`NoAuthorizationException`) | `403` | – |
| Unknown slug, username, comment (`ResourceNotFoundException`) | `404` | – |

## GraphQL API

- Endpoint: `POST /graphql` with `{"query": "...", "variables": {...}}`.
- Schema: [`src/main/resources/schema/schema.graphqls`](../src/main/resources/schema/schema.graphqls).
- Implementation: DGS components in `src/main/java/io/spring/graphql`. Types such as `Article`, `ArticlesConnection`, `DgsConstants` are generated at build time by the `generateJava` Gradle task into `build/generated`.
- GraphiQL: `/graphiql`.

### Operations

| Operation | Auth | Implemented in |
| --- | --- | --- |
| `article(slug)` | optional | `ArticleDatafetcher` |
| `articles(first, after, last, before, authoredBy, favoritedBy, withTag)` | optional | `ArticleDatafetcher` |
| `feed(first, after, last, before)` | required (see note) | `ArticleDatafetcher` |
| `me` | required | `MeDatafetcher` |
| `profile(username)` | optional | `ProfileDatafetcher` |
| `tags` | – | `TagDatafetcher` |
| `createUser(input)` → `UserResult` | – | `UserMutation` |
| `login(email, password)` | – | `UserMutation` |
| `updateUser(changes)` | required | `UserMutation` |
| `followUser(username)` / `unfollowUser(username)` | required | `RelationMutation` |
| `createArticle(input)` / `updateArticle(slug, changes)` / `deleteArticle(slug)` | required (update/delete: author) | `ArticleMutation` |
| `favoriteArticle(slug)` / `unfavoriteArticle(slug)` | required | `ArticleMutation` |
| `addComment(slug, body)` / `deleteComment(slug, id)` | required | `CommentMutation` |

Nested fields: `User.profile`, `Article.author`, `Article.comments`, `Comment.author`, `Comment.article`, `Profile.articles`, `Profile.favorites`, `Profile.feed`.

Note: `feed` (and `me`) assume an authenticated caller; called anonymously they currently fail with an `INTERNAL` error rather than an authentication error.

### Pagination

Connections follow the Relay shape (`edges { cursor node }`, `pageInfo { hasNextPage hasPreviousPage startCursor endCursor }`). Pass `first` (+ optional `after`) to page forward or `last` (+ optional `before`) to page backward; at least one of `first`/`last` must be given. Page size is capped at 1000. Cursors are the item's timestamp in epoch milliseconds (articles use `updatedAt`, comments use `createdAt`).

### Errors

`createUser` returns the `UserResult` union: `UserPayload` on success or `Error { message errors { key value } }` on validation failure. Other operations report failures in the standard GraphQL `errors` array (`GraphQLCustomizeExceptionHandler`): bad login → `UNAUTHENTICATED`, validation failures → `BAD_REQUEST` with field messages in `extensions`, anything else falls through to the DGS default handler.

### Examples

Register (note the union):

```graphql
mutation {
  createUser(input: {email: "jake@jake.jake", username: "jake", password: "jakejake"}) {
    ... on UserPayload { user { username token } }
    ... on Error { message errors { key value } }
  }
}
```

Log in:

```graphql
mutation { login(email: "jake@jake.jake", password: "jakejake") { user { username token } } }
```

Current user (send `Authorization: Token <jwt>`):

```graphql
{ me { username email profile { bio image } } }
```

Articles with a tag, newest first, then the next page:

```graphql
{
  articles(first: 10, withTag: "dragons") {
    edges { cursor node { slug title favoritesCount author { username } } }
    pageInfo { hasNextPage endCursor }
  }
}
```

```graphql
{ articles(first: 10, after: "<endCursor>") { edges { node { slug } } } }
```

Create an article and comment on it:

```graphql
mutation {
  createArticle(input: {title: "How to train your dragon", description: "Ever wonder how?", body: "You have to believe", tagList: ["dragons"]}) {
    article { slug }
  }
}
```

```graphql
mutation { addComment(slug: "how-to-train-your-dragon", body: "Nice!") { comment { id body } } }
```
