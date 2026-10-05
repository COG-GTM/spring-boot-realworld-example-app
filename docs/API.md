# API reference

The backend serves the [RealWorld API](https://realworld-docs.netlify.app/specifications/backend/introduction/) over REST and a GraphQL API with the same functionality. Both run on the same port (default `8080`) without a path prefix.

- [Conventions](#conventions)
- [REST endpoints](#rest-endpoints)
- [Errors](#errors)
- [GraphQL](#graphql)

## Conventions

- **Authentication.** Send `Authorization: Token <jwt>`. Tokens come from register, login, `GET /user`, or the GraphQL `User.token` field. They expire after `jwt.sessionTime` seconds (default 86400).
- **Body wrapping.** JSON request bodies are wrapped in a root key (`user`, `article`, `comment`), and responses use the same wrapping (`{"article": {...}}`, `{"articles": [...], "articlesCount": n}`).
- **Timestamps.** Timestamps are ISO-8601 strings in UTC, for example `2026-10-05T13:04:14.888Z`.
- **Auth column.** In the tables below, *optional* means the endpoint is public, but an authenticated caller gets viewer-specific fields (`favorited`, `following`) filled in.

## REST endpoints

### Users and authentication

| Method | Path | Auth | Request body | Success |
| --- | --- | --- | --- | --- |
| `POST` | `/users` | – | `{"user": {"email", "username", "password"}}` | `201` `{"user": {...}}` |
| `POST` | `/users/login` | – | `{"user": {"email", "password"}}` | `200` `{"user": {...}}` |
| `GET` | `/user` | required | – | `200` `{"user": {...}}` |
| `PUT` | `/user` | required | `{"user": {"email"?, "username"?, "password"?, "bio"?, "image"?}}` | `200` `{"user": {...}}` |

`user` object: `email`, `username`, `bio`, `image`, `token`. New users get the avatar from `image.default`.

### Profiles

| Method | Path | Auth | Success |
| --- | --- | --- | --- |
| `GET` | `/profiles/{username}` | optional | `200` `{"profile": {...}}` |
| `POST` | `/profiles/{username}/follow` | required | `200` `{"profile": {...}}` |
| `DELETE` | `/profiles/{username}/follow` | required | `200` `{"profile": {...}}` |

`profile` object: `username`, `bio`, `image`, `following`.

### Articles

| Method | Path | Auth | Notes | Success |
| --- | --- | --- | --- | --- |
| `GET` | `/articles` | optional | Query params: `tag`, `author`, `favorited` (username), `offset` (default 0), `limit` (default 20, max 100). Newest first. | `200` `{"articles": [...], "articlesCount": n}` |
| `GET` | `/articles/feed` | required | Articles by users the caller follows. `offset`, `limit` as above. | `200` same shape |
| `POST` | `/articles` | required | `{"article": {"title", "description", "body", "tagList"?}}`. The title must produce a unique slug. | `200` `{"article": {...}}` |
| `GET` | `/articles/{slug}` | optional | | `200` `{"article": {...}}` |
| `PUT` | `/articles/{slug}` | required, author only | `{"article": {"title"?, "description"?, "body"?}}`. A new title regenerates the slug. Tags cannot be updated. | `200` `{"article": {...}}` |
| `DELETE` | `/articles/{slug}` | required, author only | | `204` |
| `POST` | `/articles/{slug}/favorite` | required | | `200` `{"article": {...}}` |
| `DELETE` | `/articles/{slug}/favorite` | required | | `200` `{"article": {...}}` |

`article` object: `slug`, `title`, `description`, `body`, `tagList`, `createdAt`, `updatedAt`, `favorited`, `favoritesCount`, `author` (profile). The current implementation also serializes two fields that are not in the RealWorld spec: `id` and `cursor` (`{"data": "<updatedAt>"}`).

### Comments

| Method | Path | Auth | Notes | Success |
| --- | --- | --- | --- | --- |
| `GET` | `/articles/{slug}/comments` | optional | All comments, no pagination | `200` `{"comments": [...]}` |
| `POST` | `/articles/{slug}/comments` | required | `{"comment": {"body"}}` | `201` `{"comment": {...}}` |
| `DELETE` | `/articles/{slug}/comments/{id}` | required, comment author or article author | | `204` |

`comment` object: `id`, `body`, `createdAt`, `updatedAt`, `author` (profile).

### Tags

| Method | Path | Auth | Success |
| --- | --- | --- | --- |
| `GET` | `/tags` | – | `200` `{"tags": ["..."]}` |

### Example session

```bash
# Register
curl -X POST localhost:8080/users -H 'Content-Type: application/json' \
  -d '{"user":{"email":"jake@example.com","username":"jake","password":"secret12"}}'

# Log in and capture the token
TOKEN=$(curl -s -X POST localhost:8080/users/login -H 'Content-Type: application/json' \
  -d '{"user":{"email":"jake@example.com","password":"secret12"}}' | jq -r .user.token)

# Create an article
curl -X POST localhost:8080/articles -H "Authorization: Token $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"article":{"title":"Hello","description":"First post","body":"...","tagList":["intro"]}}'

# List articles by tag
curl 'localhost:8080/articles?tag=intro&limit=10'
```

## Errors

| Status | When | Body |
| --- | --- | --- |
| `401` | Missing or invalid token on a protected route | empty |
| `403` | Authenticated but not allowed (editing someone else's article or comment) | empty |
| `404` | Unknown article, comment or profile | empty |
| `422` | Validation failure | `{"errors": {"<field>": ["<message>", ...]}}` |
| `422` | Wrong email or password on login | `{"message": "invalid email or password"}` |

Validation example (`POST /users` with an invalid body):

```json
{"errors":{"email":["should be an email"],"username":["can't be empty"],"password":["can't be empty"]}}
```

## GraphQL

- **Endpoint:** `POST /graphql` (public; each resolver enforces authentication).
- **GraphiQL:** `GET /graphiql`.
- **Schema:** [`src/main/resources/schema/schema.graphqls`](../src/main/resources/schema/schema.graphqls).
- **Authentication:** send the same `Authorization: Token <jwt>` header as REST.

### Queries

| Field | Auth | Returns |
| --- | --- | --- |
| `article(slug: String!)` | optional | `Article` |
| `articles(first, after, last, before, authoredBy, favoritedBy, withTag)` | optional | `ArticlesConnection` |
| `feed(first, after, last, before)` | required | `ArticlesConnection` |
| `me` | required (returns `null` when anonymous) | `User` |
| `profile(username: String!)` | optional | `ProfilePayload` |
| `tags` | – | `[String]` |

Nested connections are also available: `Article.comments`, `Profile.articles`, `Profile.favorites` and `Profile.feed`.

### Mutations

| Mutation | Auth | Returns |
| --- | --- | --- |
| `createUser(input: CreateUserInput)` | – | `UserResult` (`UserPayload` or `Error` with per-field `errors`) |
| `login(email, password)` | – | `UserPayload` |
| `updateUser(changes: UpdateUserInput!)` | required | `UserPayload` |
| `followUser(username)` / `unfollowUser(username)` | required | `ProfilePayload` |
| `createArticle(input: CreateArticleInput!)` | required | `ArticlePayload` |
| `updateArticle(slug, changes: UpdateArticleInput!)` | required, author only | `ArticlePayload` |
| `favoriteArticle(slug)` / `unfavoriteArticle(slug)` | required | `ArticlePayload` |
| `deleteArticle(slug)` | required, author only | `DeletionStatus` |
| `addComment(slug, body)` | required | `CommentPayload` |
| `deleteComment(slug, id)` | required, comment or article author | `DeletionStatus` |

### Pagination

Connections follow the Relay shape (`edges { cursor node }`, `pageInfo { startCursor endCursor hasNextPage hasPreviousPage }`):

- Exactly one of `first` (with optional `after`) or `last` (with optional `before`) must be given. If both or neither are given, the field fails with an `IllegalArgumentException`.
- Cursors are timestamps in epoch milliseconds: `updatedAt` for articles and `createdAt` for comments.
- Page size defaults to 20 and is capped at 1000.

### Examples

```graphql
mutation {
  login(email: "jake@example.com", password: "secret12") {
    user { username token }
  }
}
```

```graphql
query {
  articles(first: 10, withTag: "intro") {
    edges {
      cursor
      node { slug title favoritesCount author { username following } }
    }
    pageInfo { hasNextPage endCursor }
  }
}
```

```graphql
mutation {
  createArticle(input: {title: "Hello", description: "First post", body: "...", tagList: ["intro"]}) {
    article { slug createdAt }
  }
}
```

### GraphQL errors

- Login failure (`InvalidAuthenticationException`) → error with `errorType: UNAUTHENTICATED`.
- Validation failure (`ConstraintViolationException`) → `BAD_REQUEST` error whose `extensions` hold the per-field messages. `createUser` instead returns the `Error` union member as data.
- Missing authentication on mutations, and other exceptions → the DGS default handler (`INTERNAL`).
