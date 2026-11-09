# API reference

This document describes the HTTP API as implemented in `src/main/java/io/spring/api` (REST) and `src/main/java/io/spring/graphql` (GraphQL). Base URL when running locally: `http://localhost:8080` (no `/api` prefix).

## Conventions

- **Authentication:** send `Authorization: Token <jwt>`. `JwtTokenFilter` takes the second space-separated part of the header as the token, so the scheme word is not checked. Tokens are HS512-signed, carry the user id as `sub`, and expire after `jwt.sessionTime` seconds (default 86400). Invalid or expired tokens are ignored (the request continues as anonymous), so protected endpoints then answer `401`.
- **Envelopes:** request bodies are wrapped in a root key (`{"user": {...}}`, `{"article": {...}}`, `{"comment": {...}}`); Jackson's `UNWRAP_ROOT_VALUE` is enabled for this. Responses use the same style.
- **Dates:** ISO-8601 in UTC, e.g. `2026-01-01T12:00:00.000Z`.
- **CORS:** all origins, methods `HEAD, GET, POST, PUT, DELETE, PATCH`, headers `Authorization, Cache-Control, Content-Type`, no credentials.

## Response objects

User (returned by register, login, `GET/PUT /user`):

```json
{ "user": { "email": "jake@jake.jake", "username": "jake", "bio": "", "image": "https://...", "token": "<jwt>" } }
```

Profile:

```json
{ "profile": { "username": "jake", "bio": "", "image": "https://...", "following": false } }
```

Article (`ArticleData`). Besides the RealWorld fields, the response also contains the internal `id` and a `cursor` object (the `Node.getCursor()` getter is serialized by Jackson):

```json
{
  "article": {
    "id": "c1f0...",
    "slug": "how-to-train-your-dragon",
    "title": "How to train your dragon",
    "description": "Ever wonder how?",
    "body": "You have to believe",
    "favorited": false,
    "favoritesCount": 0,
    "createdAt": "2026-01-01T12:00:00.000Z",
    "updatedAt": "2026-01-01T12:00:00.000Z",
    "tagList": ["dragons", "training"],
    "cursor": { "data": "2026-01-01T12:00:00.000Z" },
    "author": { "username": "jake", "bio": "", "image": "https://...", "following": false }
  }
}
```

Article list: `{ "articles": [ ...article objects... ], "articlesCount": 2 }`

Comment: `{ "comment": { "id": "...", "body": "...", "createdAt": "...", "updatedAt": "...", "cursor": { "data": "..." }, "author": { ...profile... } } }`; list: `{ "comments": [ ... ] }`

Tags: `{ "tags": ["dragons", "training"] }`

`favorited` and `following` are only computed when the request carries a valid token; otherwise they are `false`.

## REST endpoints

### Users and authentication (`UsersApi`, `CurrentUserApi`)

| Method & path | Auth | Body | Success |
| --- | --- | --- | --- |
| `POST /users` | – | `{"user": {"email", "username", "password"}}` | `201` + user |
| `POST /users/login` | – | `{"user": {"email", "password"}}` | `200` + user |
| `GET /user` | required | – | `200` + user (echoes the request token) |
| `PUT /user` | required | `{"user": {"email"?, "username"?, "password"?, "bio"?, "image"?}}` | `200` + user |

Validation: register requires non-blank `email` (valid format, not taken), `username` (not taken) and `password`. On update, empty or missing fields are left unchanged; `email` must be valid and neither `email` nor `username` may belong to another user. New users get `bio = ""` and `image = image.default`.

Login with an unknown email or wrong password returns `422 {"message": "invalid email or password"}`.

### Profiles (`ProfileApi`)

| Method & path | Auth | Success |
| --- | --- | --- |
| `GET /profiles/{username}` | optional | `200` + profile |
| `POST /profiles/{username}/follow` | required | `200` + profile (following again is a no-op) |
| `DELETE /profiles/{username}/follow` | required | `200` + profile |

Unknown username returns `404`. `DELETE .../follow` also returns `404` if you were not following the user.

### Articles (`ArticlesApi`, `ArticleApi`, `ArticleFavoriteApi`)

| Method & path | Auth | Success |
| --- | --- | --- |
| `GET /articles` | optional | `200` + article list |
| `GET /articles/feed` | required | `200` + article list |
| `POST /articles` | required | `200` + article |
| `GET /articles/{slug}` | optional | `200` + article |
| `PUT /articles/{slug}` | required, author only | `200` + article |
| `DELETE /articles/{slug}` | required, author only | `204` |
| `POST /articles/{slug}/favorite` | required | `200` + article |
| `DELETE /articles/{slug}/favorite` | required | `200` + article |

- `GET /articles` query parameters: `tag`, `author` (username), `favorited` (username of a user who favorited), `offset` (default 0), `limit` (default 20, max 100). Results are ordered by `createdAt`, newest first; `articlesCount` is the total number of matches.
- `GET /articles/feed` returns articles written by users the caller follows; it takes `offset` and `limit`.
- Create body: `{"article": {"title", "description", "body", "tagList"?}}`. `title`, `description` and `body` are required, and the slug derived from the title must not already exist (`422 {"errors": {"title": ["article name exists"]}}`).
- Update body: `{"article": {"title"?, "description"?, "body"?}}`; empty fields are ignored. Changing the title regenerates the slug.
- The slug is the title lower-cased with whitespace and some punctuation replaced by `-` (`Article.toSlug`).
- Writing someone else's article returns `403`; unknown slugs return `404`.

### Comments (`CommentsApi`)

| Method & path | Auth | Success |
| --- | --- | --- |
| `GET /articles/{slug}/comments` | optional | `200` + comment list |
| `POST /articles/{slug}/comments` | required | `201` + comment |
| `DELETE /articles/{slug}/comments/{id}` | required | `204` |

Create body: `{"comment": {"body"}}` (`body` must not be blank). A comment can be deleted by its author or by the article's author; anyone else gets `403`.

### Tags (`TagsApi`)

`GET /tags` (public) returns every tag name in the database.

### Errors and status codes

| Status | When | Body |
| --- | --- | --- |
| `401` | Protected endpoint without a valid token | empty |
| `403` | Not allowed to modify the resource (`NoAuthorizationException`) | empty |
| `404` | Unknown slug, username or comment (`ResourceNotFoundException`) | empty |
| `422` | Bean-validation failure (request body or service-level `@Valid`) | `{"errors": {"<field>": ["<message>", ...]}}` |
| `422` | Wrong login credentials | `{"message": "invalid email or password"}` |

## GraphQL

- Endpoint: `POST /graphql` with `{"query": "...", "variables": {...}}`; GraphiQL UI at `/graphiql`.
- Schema: [`src/main/resources/schema/schema.graphqls`](../src/main/resources/schema/schema.graphqls). Java types for it are generated by the DGS codegen plugin (`generateJava` task) into package `io.spring.graphql`.
- Authentication uses the same `Authorization: Token <jwt>` header. `/graphql` itself is public; resolvers that need a user throw `AuthenticationException` when there is none.

### Queries

| Field | Arguments | Notes |
| --- | --- | --- |
| `article` | `slug: String!` | |
| `articles` | `first, after, last, before, authoredBy, favoritedBy, withTag` | Connection |
| `feed` | `first, after, last, before` | Articles by followed users; needs a token |
| `me` | – | Current user; needs the `Authorization` header |
| `profile` | `username: String!` | Returns `ProfilePayload` |
| `tags` | – | `[String]` |

Nested connections: `Article.comments`, `Profile.articles`, `Profile.favorites`, `Profile.feed` (all take `first/after/last/before`).

### Mutations

| Field | Arguments | Returns | Auth |
| --- | --- | --- | --- |
| `createUser` | `input: CreateUserInput {email, username, password}` | `UserResult` (`UserPayload` or `Error`) | – |
| `login` | `email, password` | `UserPayload` | – |
| `updateUser` | `changes: UpdateUserInput {email, username, password, image, bio}` | `UserPayload` | required |
| `followUser` / `unfollowUser` | `username` | `ProfilePayload` | required |
| `createArticle` | `input: CreateArticleInput {title, description, body, tagList}` | `ArticlePayload` | required |
| `updateArticle` | `slug, changes: UpdateArticleInput {title, description, body}` | `ArticlePayload` | author |
| `favoriteArticle` / `unfavoriteArticle` | `slug` | `ArticlePayload` | required |
| `deleteArticle` | `slug` | `DeletionStatus {success}` | author |
| `addComment` | `slug, body` | `CommentPayload` | required |
| `deleteComment` | `slug, id` | `DeletionStatus {success}` | comment or article author |

`UserPayload.user.token` contains a freshly issued JWT.

### Pagination

Connections follow the Relay shape (`edges { cursor node }`, `pageInfo { startCursor endCursor hasNextPage hasPreviousPage }`). Cursors are epoch-millisecond timestamps. Pass `first` (optionally with `after`) to page forward from the newest items, or `last` (optionally with `before`) to page backward. At least one of `first`/`last` is required; if both are given, `first` wins.

### Errors

- Wrong login credentials: GraphQL error with `errorType: UNAUTHENTICATED`.
- Validation failures (`ConstraintViolationException`): GraphQL error of type `BAD_REQUEST` whose `extensions` map field names to messages. `createUser` instead returns them as data: `Error { message: "BAD_REQUEST", errors: [{ key, value }] }`.
- Other exceptions go through the DGS default handler.

### Example

```bash
curl -s http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createUser(input: {email: \"jake@jake.jake\", username: \"jake\", password: \"jakejake\"}) { ... on UserPayload { user { username token } } ... on Error { message errors { key value } } } }"}'
```
