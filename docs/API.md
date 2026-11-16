# API reference

Base URL: `http://localhost:8080` (no `/api` prefix). The REST API follows the [RealWorld API spec](https://github.com/gothinkster/realworld); this page describes what this implementation actually does, including where it differs from the spec.

## Authentication

- Register (`POST /users`) or log in (`POST /users/login`) to get a JWT in `user.token`. GraphQL `createUser` / `login` return the same token.
- Send it as `Authorization: Token <jwt>`. `JwtTokenFilter` uses the second space-separated part of the header and ignores the scheme word, so `Bearer <jwt>` also works.
- Tokens are HS512-signed with `jwt.secret`, carry the user id as `sub`, and expire after `jwt.sessionTime` seconds (default 86400). There is no refresh or logout endpoint.
- Passwords are hashed with BCrypt.
- An invalid or expired token is ignored (the request continues as anonymous). Protected REST endpoints then return `401` with an empty body.

### Access rules (`WebSecurityConfig`)

| Rule | Access |
| --- | --- |
| `OPTIONS` any path | public |
| `/graphql`, `/graphiql` | public (each GraphQL resolver checks auth itself) |
| `GET /articles/feed` | authenticated |
| `POST /users`, `POST /users/login` | public |
| `GET /articles/**`, `GET /profiles/**`, `GET /tags` | public; the current user is used when a token is sent |
| anything else | authenticated |

Ownership checks (`core.service.AuthorizationService`): only the article author can update or delete an article; a comment can be deleted by its author or by the article's author. Failing a check returns `403`.

## Request and response format

- Request bodies are wrapped in a root key (`spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true`): `{"user": {...}}`, `{"article": {...}}`, `{"comment": {...}}`.
- Responses are wrapped the same way: `{"user": ...}`, `{"profile": ...}`, `{"article": ...}`, `{"articles": [...], "articlesCount": n}`, `{"comment": ...}`, `{"comments": [...]}`, `{"tags": [...]}`.
- Dates are ISO-8601 in UTC with milliseconds, e.g. `2026-11-16T13:04:33.863Z`.
- Differences from the spec: article objects also contain `id` and `cursor`, and comment objects contain `cursor` (an object like `{"data": "<timestamp>"}`). These come from `ArticleData`/`CommentData` implementing the internal `Node` interface.

### Errors

| Situation | Status | Body |
| --- | --- | --- |
| Validation failure (bean validation on the body, or service-level constraints such as duplicate email/username/title) | `422` | `{"errors": {"<field>": ["<message>", ...]}}` |
| Wrong email or password on login | `422` | `{"message": "invalid email or password"}` |
| Missing/invalid token on a protected endpoint | `401` | empty |
| Not the owner (`NoAuthorizationException`) | `403` | Spring default error body |
| Unknown slug, username or comment (`ResourceNotFoundException`) | `404` | Spring default error body |

Example: `POST /users` with an existing email and an empty username returns

```json
{"errors":{"email":["duplicated email"],"username":["can't be empty"]}}
```

## REST endpoints

### Users

| Method | Path | Auth | Body | Response |
| --- | --- | --- | --- | --- |
| POST | `/users` | no | `{"user":{"email","username","password"}}` | `201`, `{"user":{email,username,bio,image,token}}` |
| POST | `/users/login` | no | `{"user":{"email","password"}}` | `200`, `{"user":{...}}` |
| GET | `/user` | yes | | `200`, `{"user":{...}}` (echoes the token from the request header) |
| PUT | `/user` | yes | `{"user":{"email"?,"username"?,"password"?,"bio"?,"image"?}}` | `200`, `{"user":{...}}` |

Validation (`RegisterParam`, `LoginParam`, `UpdateUserParam`, `UpdateUserCommand`):

- Register: `email` required, valid email, unique; `username` required, unique; `password` required.
- Login: `email` required and a valid email; `password` required.
- Update: only non-empty fields are changed. `email` must be a valid email; `email` and `username` must not belong to another user ("email already exist" / "username already exist"). A new password is hashed before it is stored.
- New users get `bio: ""` and `image` set to `image.default`.

### Profiles

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| GET | `/profiles/{username}` | optional | `{"profile":{username,bio,image,following}}` |
| POST | `/profiles/{username}/follow` | yes | `{"profile":{...}}` with `following: true`; following twice is a no-op |
| DELETE | `/profiles/{username}/follow` | yes | `{"profile":{...}}`; returns `404` if you were not following the user |

### Articles

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/articles` | optional | Query params: `tag`, `author` (username), `favorited` (username), `offset` (default 0), `limit` (default 20, capped at 100). Newest first. Returns `{"articles":[...],"articlesCount":n}` where `articlesCount` is the total number of matches |
| GET | `/articles/feed` | yes | Articles by users you follow; `offset`, `limit`. Empty list and `articlesCount: 0` if you follow nobody |
| POST | `/articles` | yes | `{"article":{"title","description","body","tagList"?}}`; `title`, `description`, `body` required; a title whose slug already exists is rejected with `"article name exists"`. Returns `200` |
| GET | `/articles/{slug}` | optional | `{"article":{...}}` |
| PUT | `/articles/{slug}` | author | `{"article":{"title"?,"description"?,"body"?}}`; only non-empty fields change; a new title regenerates the slug. Tags cannot be changed |
| DELETE | `/articles/{slug}` | author | `204` |
| POST | `/articles/{slug}/favorite` | yes | Returns the article; favoriting twice is a no-op |
| DELETE | `/articles/{slug}/favorite` | yes | Returns the article; no-op if not favorited |

Article object:

```json
{
  "id": "c0f21607-...",
  "slug": "hello-world",
  "title": "Hello World",
  "description": "d",
  "body": "b",
  "favorited": false,
  "favoritesCount": 0,
  "createdAt": "2026-11-16T13:04:33.863Z",
  "updatedAt": "2026-11-16T13:04:33.863Z",
  "tagList": ["x", "y"],
  "cursor": {"data": "2026-11-16T13:04:33.863Z"},
  "author": {"username": "alice", "bio": "", "image": "https://...", "following": false}
}
```

Slugs come from `Article.toSlug`: the title is lower-cased and runs of whitespace, `?`, `,`, `.`, `&`, `|`, curly quotes and some full-width characters become `-`. Tags are de-duplicated per article and stored once globally by name.

### Comments

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/articles/{slug}/comments` | optional | `{"comments":[...]}` |
| POST | `/articles/{slug}/comments` | yes | `{"comment":{"body"}}`; `body` required. Returns `201`, `{"comment":{id,body,createdAt,updatedAt,cursor,author}}` |
| DELETE | `/articles/{slug}/comments/{id}` | comment author or article author | `204` |

### Tags

`GET /tags` returns `{"tags":[...]}`: every name in the `tags` table, including tags whose articles were deleted.

## GraphQL

- `POST /graphql` with `{"query": "...", "variables": {...}}`; GraphiQL at `/graphiql`.
- Schema: [`src/main/resources/schema/schema.graphqls`](../src/main/resources/schema/schema.graphqls).
- Send the same `Authorization: Token <jwt>` header for authenticated operations.

### Queries

| Field | Arguments | Auth | Returns |
| --- | --- | --- | --- |
| `article` | `slug: String!` | optional | `Article` |
| `articles` | `first, after, last, before, authoredBy, favoritedBy, withTag` | optional | `ArticlesConnection` |
| `feed` | `first, after, last, before` | required (see known issues) | `ArticlesConnection` |
| `me` | | required | `User` |
| `profile` | `username: String!` | optional | `ProfilePayload` |
| `tags` | | no | `[String]` |

Nested connections: `Article.comments`, `Profile.articles`, `Profile.favorites`, `Profile.feed` (all take `first, after, last, before`).

### Mutations

| Mutation | Arguments | Returns |
| --- | --- | --- |
| `createUser` | `input: CreateUserInput {email, username, password}` | `UserResult` = `UserPayload` or `Error` |
| `login` | `email: String!, password: String!` | `UserPayload` |
| `updateUser` | `changes: UpdateUserInput! {email, username, password, image, bio}` | `UserPayload` |
| `followUser` / `unfollowUser` | `username: String!` | `ProfilePayload` |
| `createArticle` | `input: CreateArticleInput! {title!, description!, body!, tagList}` | `ArticlePayload` |
| `updateArticle` | `slug: String!, changes: UpdateArticleInput! {title, description, body}` | `ArticlePayload` |
| `favoriteArticle` / `unfavoriteArticle` | `slug: String!` | `ArticlePayload` |
| `deleteArticle` | `slug: String!` | `DeletionStatus {success}` |
| `addComment` | `slug: String!, body: String!` | `CommentPayload` |
| `deleteComment` | `slug: String!, id: ID!` | `DeletionStatus {success}` |

All mutations except `createUser` and `login` need a token; without one they fail with an error (`AuthenticationException`). `updateUser` returns `null` instead.

### Errors

- `createUser` returns validation problems as data: `{"__typename": "Error", "message": "BAD_REQUEST", "errors": [{"key": "email", "value": ["duplicated email"]}]}`.
- Other validation failures (`ConstraintViolationException`) become a `BAD_REQUEST` GraphQL error with the field messages in `extensions`.
- Wrong credentials on `login` become an `UNAUTHENTICATED` error.
- Anything else, including not-found and permission errors, uses DGS's default handler (`INTERNAL` error type with the exception as message).

### Pagination

Connections are cursor-based. Pass `first` (and optionally `after`) to page forward, or `last` (and optionally `before`) to page backward; `first` wins if both are given. Omitting both is an error. Page size is capped at 1000. Cursors are epoch milliseconds as strings (e.g. `"1794834273863"`); use `pageInfo.endCursor` / `startCursor`.

### Examples

```graphql
mutation { login(email: "a@b.c", password: "pw") { user { username token } } }
```

```graphql
mutation {
  createArticle(input: {title: "GQL Post", description: "d", body: "b", tagList: ["g"]}) {
    article { slug favoritesCount author { username } }
  }
}
```

```graphql
{
  articles(first: 10, withTag: "g") {
    edges { cursor node { slug title comments(first: 5) { edges { node { body } } } } }
    pageInfo { hasNextPage endCursor }
  }
}
```

### Known issues

See [ARCHITECTURE.md#known-issues](ARCHITECTURE.md#known-issues). The ones most visible to API callers:

- `feed` without a token returns an `INTERNAL` `NullPointerException` error instead of an auth error.
- `me` without an `Authorization` header returns an `INTERNAL` error ("Required header 'Authorization' was not provided") instead of `null`.
- Omitting both `first` and `last` returns an error whose message is in Chinese (`first 和 last 必须只存在一个`).
- `Article.comments` ignores the page size.
