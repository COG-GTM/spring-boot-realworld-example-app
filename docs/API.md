# API reference

The application exposes the same functionality through REST (RealWorld spec) and GraphQL (Netflix DGS). Both run on port `8080` and share the same services, so data created through one API is visible in the other.

## Authentication

- Obtain a token from `POST /users`, `POST /users/login`, or the GraphQL `createUser` / `login` mutations.
- Send it as `Authorization: Token <jwt>`. `JwtTokenFilter` reads the second whitespace-separated part of the header, so the scheme word is not checked.
- Tokens are HS512-signed JWTs whose subject (`sub`) is the user id; they expire after `jwt.sessionTime` seconds (default 86400).
- An invalid or expired token is ignored (the request continues as anonymous); endpoints that need a user then return `401`.

Access rules come from `WebSecurityConfig`:

| Rule | Paths |
| --- | --- |
| Public | `OPTIONS *`, `/graphql`, `/graphiql`, `POST /users`, `POST /users/login`, `GET /articles/**` (except `/articles/feed`), `GET /profiles/**`, `GET /tags` |
| Authenticated | `GET /articles/feed` and everything else |

GraphQL is public at the HTTP level; each mutation checks for a current user itself.

## REST

All request and response bodies are JSON wrapped in a root key (`user`, `article`, `comment`, `profile`), as required by the spec (`spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true`).

### Users

| Method | Path | Auth | Body | Response |
| --- | --- | --- | --- | --- |
| POST | `/users` | – | `{"user": {"email", "username", "password"}}` | `201` `{"user": User}` |
| POST | `/users/login` | – | `{"user": {"email", "password"}}` | `200` `{"user": User}` |
| GET | `/user` | required | – | `200` `{"user": User}` |
| PUT | `/user` | required | `{"user": {"email"?, "username"?, "password"?, "bio"?, "image"?}}` | `200` `{"user": User}` |

`User` = `{email, username, bio, image, token}`. `/user` echoes back the token from the request header.

Registration validation: email required and well-formed, username required, password required, email and username unique. `PUT /user` only changes non-empty fields and rejects an email/username that belongs to someone else. Wrong login credentials return `422` `{"message": "invalid email or password"}`.

### Profiles

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| GET | `/profiles/{username}` | optional | `{"profile": Profile}` |
| POST | `/profiles/{username}/follow` | required | `{"profile": Profile}` |
| DELETE | `/profiles/{username}/follow` | required | `{"profile": Profile}` (`404` if not following) |

`Profile` = `{username, bio, image, following}`.

### Articles

| Method | Path | Auth | Body / params | Response |
| --- | --- | --- | --- | --- |
| GET | `/articles` | optional | `tag`, `author`, `favorited` (username), `offset` (default 0), `limit` (default 20, max 100) | `{"articles": [Article], "articlesCount": n}` |
| GET | `/articles/feed` | required | `offset`, `limit` | same as above, articles by followed authors |
| POST | `/articles` | required | `{"article": {"title", "description", "body", "tagList"?}}` | `200` `{"article": Article}` |
| GET | `/articles/{slug}` | optional | – | `{"article": Article}` |
| PUT | `/articles/{slug}` | author only | `{"article": {"title"?, "description"?, "body"?}}` | `{"article": Article}` |
| DELETE | `/articles/{slug}` | author only | – | `204` |
| POST | `/articles/{slug}/favorite` | required | – | `{"article": Article}` |
| DELETE | `/articles/{slug}/favorite` | required | – | `{"article": Article}` |

`Article` = `{slug, title, description, body, tagList, createdAt, updatedAt, favorited, favoritesCount, author: Profile}`. The current serialization also includes the internal `id` and a `cursor` object (see `ArticleData`). Timestamps are ISO-8601 in UTC (`JacksonCustomizations`).

Notes from the code:

- The slug is derived from the title (`Article.toSlug`: lower-cased, whitespace and some punctuation replaced by `-`). Creating an article whose slug already exists fails validation (`title: ["article name exists"]`). Changing the title on update also changes the slug.
- `GET /articles` is ordered by `created_at` descending. Duplicate tags in `tagList` are collapsed.
- `favorited`/`following` are only computed when a user is authenticated; otherwise they are `false`.

### Comments

| Method | Path | Auth | Body | Response |
| --- | --- | --- | --- | --- |
| GET | `/articles/{slug}/comments` | optional | – | `{"comments": [Comment]}` |
| POST | `/articles/{slug}/comments` | required | `{"comment": {"body"}}` | `201` `{"comment": Comment}` |
| DELETE | `/articles/{slug}/comments/{id}` | comment author or article author | – | `204` |

`Comment` = `{id, body, createdAt, updatedAt, author: Profile}`.

### Tags

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| GET | `/tags` | – | `{"tags": ["..."]}` (all tags that exist in the `tags` table) |

### Errors

| Status | When | Body |
| --- | --- | --- |
| `401` | No/invalid token on a protected route | empty |
| `403` | Not the owner (`NoAuthorizationException`) | empty |
| `404` | Unknown slug, username or comment (`ResourceNotFoundException`) | empty |
| `422` | Bean validation failure | `{"errors": {"<field>": ["<message>", ...]}}` |
| `422` | Wrong login credentials | `{"message": "invalid email or password"}` |

## GraphQL

- Endpoint: `POST /graphql`, body `{"query": "...", "variables": {...}}`.
- Schema: [`src/main/resources/schema/schema.graphqls`](../src/main/resources/schema/schema.graphqls).
- Implementation: `src/main/java/io/spring/graphql` (`@DgsComponent` classes). Generated types (`io.spring.graphql.types.*`, `DgsConstants`) come from the DGS codegen `generateJava` task and are not checked in.

### Queries

| Field | Arguments | Returns | Fetcher |
| --- | --- | --- | --- |
| `article` | `slug: String!` | `Article` | `ArticleDatafetcher.findArticleBySlug` |
| `articles` | `first, after, last, before, authoredBy, favoritedBy, withTag` | `ArticlesConnection` | `ArticleDatafetcher.getArticles` |
| `feed` | `first, after, last, before` | `ArticlesConnection` | `ArticleDatafetcher.getFeed` |
| `me` | – | `User` | `MeDatafetcher.getMe` |
| `profile` | `username: String!` | `ProfilePayload` | `ProfileDatafetcher.queryProfile` |
| `tags` | – | `[String]` | `TagDatafetcher.getTags` |

Nested resolvers: `Article.author`, `Comment.author`, `User.profile` (`ProfileDatafetcher`); `Article.comments` (`CommentDatafetcher`); `Comment.article` (`ArticleDatafetcher`); `Profile.articles`, `Profile.favorites`, `Profile.feed` (`ArticleDatafetcher`).

### Mutations

| Field | Arguments | Returns | Auth |
| --- | --- | --- | --- |
| `createUser` | `input: CreateUserInput {email, username, password}` | `UserResult` (`UserPayload` \| `Error`) | – |
| `login` | `email, password` | `UserPayload` | – |
| `updateUser` | `changes: UpdateUserInput {email, username, password, image, bio}` | `UserPayload` | required |
| `followUser` / `unfollowUser` | `username` | `ProfilePayload` | required |
| `createArticle` | `input: CreateArticleInput {title, description, body, tagList}` | `ArticlePayload` | required |
| `updateArticle` | `slug, changes: UpdateArticleInput {title, description, body}` | `ArticlePayload` | author |
| `favoriteArticle` / `unfavoriteArticle` | `slug` | `ArticlePayload` | required |
| `deleteArticle` | `slug` | `DeletionStatus {success}` | author |
| `addComment` | `slug, body` | `CommentPayload` | required |
| `deleteComment` | `slug, id` | `DeletionStatus` | comment or article author |

`UserPayload.user.token` is a freshly issued JWT.

### Pagination

Connection fields follow the Relay shape (`edges { cursor node }`, `pageInfo { startCursor endCursor hasNextPage hasPreviousPage }`):

- Pass `first` (optionally with `after`) to page forward, or `last` (optionally with `before`) to page backward. Supplying neither is rejected with an error. Page size is capped at 100.
- Cursors are the epoch-millisecond string of a timestamp (`DateTimeCursor`).
- Only the direction you are paging in is reported: `hasNextPage` when using `first`, `hasPreviousPage` when using `last`.

### Errors

- Bean validation failures in mutations are returned as GraphQL errors with `errorType: BAD_REQUEST` and the per-field messages in `extensions`, except `createUser`, which returns them as data through the `Error` type (`{message: "BAD_REQUEST", errors: [{key, value}]}`).
- Invalid login credentials produce an `UNAUTHENTICATED` error.
- Other exceptions (not found, missing authentication, forbidden, invalid paging arguments) go through DGS's default handler and are currently reported as `INTERNAL` errors.

### Example

```bash
# Log in and read the token
curl -s http://localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"mutation { login(email: \"jake@example.com\", password: \"secret\") { user { username token } } }"}'

# Create an article
curl -s http://localhost:8080/graphql -H 'Content-Type: application/json' \
  -H "Authorization: Token $TOKEN" \
  -d '{"query":"mutation($in: CreateArticleInput!) { createArticle(input: $in) { article { slug title tagList } } }","variables":{"in":{"title":"Hello","description":"d","body":"b","tagList":["java"]}}}'
```
