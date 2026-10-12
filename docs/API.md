# API reference

The backend exposes the same functionality through a REST API (RealWorld spec) and a GraphQL API. Both run on the same port (default `8080`). This document reflects the controllers in `src/main/java/io/spring/api` and the datafetchers in `src/main/java/io/spring/graphql`.

## Authentication

Register or log in to receive a JWT, then send it on subsequent requests:

```
Authorization: Token <jwt>
```

`JwtTokenFilter` reads the second whitespace-separated part of the header, so the scheme word is not validated (`Bearer <jwt>` also works). Tokens are HS512-signed, carry the user id as `sub`, and expire after `jwt.sessionTime` seconds (default 86400).

Access rules (`WebSecurityConfig`):

| Rule | Paths |
| --- | --- |
| Public | `OPTIONS *`, `/graphql`, `/graphiql`, `POST /users`, `POST /users/login`, `GET /articles/**` (except feed), `GET /profiles/**`, `GET /tags` |
| Authenticated | `GET /articles/feed` and every other request |

Unauthenticated requests to protected REST endpoints return **401**. `/graphql` is public at the HTTP level; individual mutations enforce authentication themselves (see below).

CORS allows any origin with methods `HEAD, GET, POST, PUT, DELETE, PATCH` and headers `Authorization, Cache-Control, Content-Type`.

---

## REST API

Request bodies are wrapped in a root object (`{"user": {...}}`, `{"article": {...}}`, `{"comment": {...}}`) because `UNWRAP_ROOT_VALUE` is enabled. Timestamps are ISO-8601 in UTC (e.g. `2026-01-01T12:00:00.000Z`).

### Users

#### `POST /users` — register

```json
{ "user": { "email": "jake@jake.jake", "username": "jake", "password": "jakejake" } }
```

Validation: all fields required, `email` must be an email, `email` and `username` must be unique. Returns **201**:

```json
{
  "user": {
    "email": "jake@jake.jake",
    "username": "jake",
    "bio": "",
    "image": "https://static.productionready.io/images/smiley-cyrus.jpg",
    "token": "<jwt>"
  }
}
```

#### `POST /users/login`

```json
{ "user": { "email": "jake@jake.jake", "password": "jakejake" } }
```

Returns **200** with the same `user` object. Wrong credentials return **422** `{"message": "invalid email or password"}`.

#### `GET /user` — current user (auth)

Returns `{"user": {...}}`; the `token` echoes the token from the request header.

#### `PUT /user` — update current user (auth)

```json
{ "user": { "email": "...", "username": "...", "password": "...", "bio": "...", "image": "..." } }
```

All fields optional; empty/missing fields are left unchanged. `email` must be a valid email; email/username already used by another user are rejected with 422.

### Profiles

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| GET | `/profiles/{username}` | optional | `{"profile": {...}}` |
| POST | `/profiles/{username}/follow` | required | `{"profile": {...}}` (idempotent) |
| DELETE | `/profiles/{username}/follow` | required | `{"profile": {...}}`; **404** if the user is unknown or not currently followed |

```json
{ "profile": { "username": "jake", "bio": "", "image": "...", "following": false } }
```

### Articles

#### `GET /articles` — list (auth optional)

Query params: `tag`, `author` (username), `favorited` (username), `offset` (default 0), `limit` (default 20, capped at 100). Results are ordered by `created_at` descending.

```json
{
  "articles": [
    {
      "slug": "how-to-train-your-dragon",
      "title": "How to train your dragon",
      "description": "Ever wonder how?",
      "body": "It takes a Jacobian",
      "tagList": ["dragons", "training"],
      "createdAt": "...",
      "updatedAt": "...",
      "favorited": false,
      "favoritesCount": 0,
      "author": { "username": "jake", "bio": "", "image": "...", "following": false }
    }
  ],
  "articlesCount": 1
}
```

Note: the article `id` is also serialized in each article object.

#### `GET /articles/feed` — articles from followed users (auth)

Query params `offset`, `limit`; same response shape.

#### `POST /articles` — create (auth)

```json
{ "article": { "title": "...", "description": "...", "body": "...", "tagList": ["a", "b"] } }
```

`title`, `description`, `body` are required; a title whose slug already exists is rejected (422, `"article name exists"`). Returns **200** `{"article": {...}}`. The slug is derived from the title (lower-cased, whitespace and some punctuation replaced by `-`).

#### `GET /articles/{slug}` (auth optional) → `{"article": {...}}` or 404

#### `PUT /articles/{slug}` — update (auth, author only)

```json
{ "article": { "title": "...", "description": "...", "body": "..." } }
```

Fields are optional. Changing the title regenerates the slug. Non-authors get **403**.

#### `DELETE /articles/{slug}` (auth, author only) → **204**

#### `POST|DELETE /articles/{slug}/favorite` (auth) → `{"article": {...}}`

### Comments

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| GET | `/articles/{slug}/comments` | optional | `{"comments": [...]}` |
| POST | `/articles/{slug}/comments` | required | **201** `{"comment": {...}}`; body `{"comment": {"body": "..."}}` (required) |
| DELETE | `/articles/{slug}/comments/{id}` | required | **204**; allowed for the comment author or the article author, otherwise **403** |

```json
{ "comment": { "id": "<uuid>", "body": "...", "createdAt": "...", "updatedAt": "...", "author": { "username": "...", "bio": "", "image": "...", "following": false } } }
```

### Tags

`GET /tags` → `{"tags": ["dragons", "training"]}`

### Errors

| Status | When | Body |
| --- | --- | --- |
| 401 | Missing/invalid token on a protected endpoint | empty |
| 403 | Not the owner (`NoAuthorizationException`) | Spring default error body |
| 404 | Unknown slug/username/comment (`ResourceNotFoundException`) | Spring default error body |
| 422 | Bean validation failure | `{"errors": {"<field>": ["<message>", ...]}}` |
| 422 | Bad login | `{"message": "invalid email or password"}` |

Example validation error:

```json
{ "errors": { "email": ["should be an email"], "username": ["can't be empty"] } }
```

---

## GraphQL API

- Endpoint: `POST /graphql` (`{"query": "...", "variables": {...}}`)
- IDE: `GET /graphiql`
- Schema: [`src/main/resources/schema/schema.graphqls`](../src/main/resources/schema/schema.graphqls)
- Auth: same `Authorization: Token <jwt>` header.

### Queries

| Field | Arguments | Notes |
| --- | --- | --- |
| `article` | `slug: String!` | Article by slug |
| `articles` | `first, after, last, before, authoredBy, favoritedBy, withTag` | `ArticlesConnection` |
| `feed` | `first, after, last, before` | Articles by users the caller follows; requires a token |
| `me` | – | Current `User`; requires the `Authorization` header |
| `profile` | `username: String!` | `ProfilePayload` |
| `tags` | – | `[String]` |

Nested connection fields: `Article.comments`, `Profile.articles` (authored), `Profile.favorites`, `Profile.feed` (articles from users that profile follows).

### Mutations

| Field | Arguments | Returns | Auth |
| --- | --- | --- | --- |
| `createUser` | `input: CreateUserInput` (`email`, `username`, `password`) | `UserResult` (`UserPayload` or `Error`) | – |
| `login` | `email`, `password` | `UserPayload` | – |
| `updateUser` | `changes: UpdateUserInput!` | `UserPayload` (null if anonymous) | required |
| `followUser` / `unfollowUser` | `username` | `ProfilePayload` | required |
| `createArticle` | `input: CreateArticleInput!` | `ArticlePayload` | required |
| `updateArticle` | `slug`, `changes: UpdateArticleInput!` | `ArticlePayload` | author |
| `favoriteArticle` / `unfavoriteArticle` | `slug` | `ArticlePayload` | required |
| `deleteArticle` | `slug` | `DeletionStatus` | author |
| `addComment` | `slug`, `body` | `CommentPayload` | required |
| `deleteComment` | `slug`, `id` | `DeletionStatus` | comment or article author |

### Pagination

Connections follow the Relay shape:

```graphql
articles(first: 10, after: "<cursor>") {
  edges { cursor node { slug title } }
  pageInfo { hasNextPage hasPreviousPage startCursor endCursor }
}
```

- Pass either `first` (+ optional `after`) to page forward or `last` (+ optional `before`) to page backward. Supplying neither raises an error; if both are given, `first` wins.
- Page size is capped at 1000.
- Cursors are epoch-millisecond timestamps (strings). Article cursors are taken from `updatedAt`, comment cursors from `createdAt`; the underlying queries filter and order on `created_at`.
- `hasNextPage` is only computed when paging forward and `hasPreviousPage` only when paging backward.

### Examples

Register and read back the token:

```graphql
mutation {
  createUser(input: { email: "jake@jake.jake", username: "jake", password: "jakejake" }) {
    ... on UserPayload { user { username token } }
    ... on Error { message errors { key value } }
  }
}
```

Create an article (with `Authorization: Token <jwt>`):

```graphql
mutation {
  createArticle(input: { title: "Hello", description: "First post", body: "...", tagList: ["intro"] }) {
    article { slug createdAt author { username } }
  }
}
```

Article with first page of comments:

```graphql
{
  article(slug: "hello") {
    title
    favoritesCount
    comments(first: 20) { edges { node { id body author { username } } } pageInfo { hasNextPage endCursor } }
  }
}
```

### Errors

`GraphQLCustomizeExceptionHandler` maps:

- `InvalidAuthenticationException` (bad login) → error with `errorType: UNAUTHENTICATED`
- `ConstraintViolationException` → `BAD_REQUEST` error whose `extensions` map field names to messages. For `createUser`, validation errors are instead returned as data through the `Error` member of the `UserResult` union.
- anything else → DGS default handling (typically `errorType: INTERNAL`).
