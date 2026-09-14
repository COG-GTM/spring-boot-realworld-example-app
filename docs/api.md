# API reference

The backend implements the [RealWorld API spec](https://realworld-docs.netlify.app/specifications/backend/endpoints/) over REST and exposes the same domain over GraphQL. Base URL is `http://localhost:8080` (no `/api` prefix).

## Authentication

Register or login to obtain a JWT, then send it on subsequent requests:

```
Authorization: Token <jwt>
```

Tokens expire after `jwt.sessionTime` seconds (default 86400). Protected REST endpoints return `401` without a valid token; GraphQL requests are always accepted at the HTTP level and individual resolvers raise errors when authentication is required.

## REST

Request bodies are wrapped in a root object (`{"user": {...}}`, `{"article": {...}}`, `{"comment": {...}}`) as in the RealWorld spec. Timestamps are ISO-8601 UTC strings.

### Users

| Method | Path | Auth | Body | Response |
| --- | --- | --- | --- | --- |
| POST | `/users` | – | `{"user": {"username", "email", "password"}}` | `201 {"user": {email, username, bio, image, token}}` |
| POST | `/users/login` | – | `{"user": {"email", "password"}}` | `200 {"user": {...}}`; `422 {"message": "invalid email or password"}` on failure |
| GET | `/user` | required | – | `200 {"user": {...}}` (token echoed from the request header) |
| PUT | `/user` | required | `{"user": {"email"?, "username"?, "password"?, "bio"?, "image"?}}` | `200 {"user": {...}}` |

Empty/null fields on `PUT /user` are ignored; duplicate email/username yield `422`.

### Profiles

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| GET | `/profiles/{username}` | optional | `200 {"profile": {username, bio, image, following}}` |
| POST | `/profiles/{username}/follow` | required | `200 {"profile": {...}}` |
| DELETE | `/profiles/{username}/follow` | required | `200 {"profile": {...}}`; `404` if not currently following |

### Articles

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/articles` | optional | Query params `tag`, `author`, `favorited`, `offset` (0), `limit` (20). Response `{"articles": [...], "articlesCount": n}` |
| GET | `/articles/feed` | required | Articles by followed users. `offset`, `limit`. Same response shape |
| POST | `/articles` | required | Body `{"article": {"title", "description", "body", "tagList"?}}`. Response `200 {"article": {...}}`. Duplicate title → `422` |
| GET | `/articles/{slug}` | optional | `200 {"article": {...}}` or `404` |
| PUT | `/articles/{slug}` | author only | Body `{"article": {"title"?, "description"?, "body"?}}`. Changing the title regenerates the slug. `403` if not the author |
| DELETE | `/articles/{slug}` | author only | `204` |

Article object: `slug, title, description, body, tagList, createdAt, updatedAt, favorited, favoritesCount, author{username, bio, image, following}`.

### Favorites

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| POST | `/articles/{slug}/favorite` | required | `200 {"article": {...}}` |
| DELETE | `/articles/{slug}/favorite` | required | `200 {"article": {...}}` |

### Comments

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/articles/{slug}/comments` | optional | `200 {"comments": [...]}` |
| POST | `/articles/{slug}/comments` | required | Body `{"comment": {"body"}}`. Response `201 {"comment": {...}}` |
| DELETE | `/articles/{slug}/comments/{id}` | comment author or article author | `204`; `403` otherwise |

Comment object: `id, body, createdAt, updatedAt, author{...}`.

### Tags

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| GET | `/tags` | – | `200 {"tags": ["..."]}` |

### Error format

Validation errors and failed login return `422`:

```json
{ "errors": { "email": ["can't be empty"], "title": ["article name exists"] } }
```

`404` for unknown slugs/usernames/comments, `403` for ownership violations, `401` for missing/invalid tokens on protected routes.

## GraphQL

* Endpoint: `POST /graphql`
* Interactive IDE: `GET /graphiql`
* Schema: [`src/main/resources/schema/schema.graphqls`](../src/main/resources/schema/schema.graphqls)

### Queries

| Field | Arguments | Returns |
| --- | --- | --- |
| `article` | `slug: String!` | `Article` |
| `articles` | `first, after, last, before, authoredBy, favoritedBy, withTag` | `ArticlesConnection` |
| `feed` | `first, after, last, before` | `ArticlesConnection` (requires auth) |
| `me` | – | `User` (`null` when unauthenticated) |
| `profile` | `username: String!` | `ProfilePayload` |
| `tags` | – | `[String]` |

`Profile` additionally exposes `articles`, `favorites` and `feed` connections; `Article` exposes a `comments` connection.

### Mutations

| Field | Arguments | Returns |
| --- | --- | --- |
| `createUser` | `input: CreateUserInput {email, username, password}` | `UserResult` (`UserPayload \| Error`) |
| `login` | `email: String!, password: String!` | `UserPayload` |
| `updateUser` | `changes: UpdateUserInput!` | `UserPayload` |
| `followUser` / `unfollowUser` | `username: String!` | `ProfilePayload` |
| `createArticle` | `input: CreateArticleInput! {title, description, body, tagList?}` | `ArticlePayload` |
| `updateArticle` | `slug: String!, changes: UpdateArticleInput!` | `ArticlePayload` |
| `favoriteArticle` / `unfavoriteArticle` | `slug: String!` | `ArticlePayload` |
| `deleteArticle` | `slug: String!` | `DeletionStatus {success}` |
| `addComment` | `slug: String!, body: String!` | `CommentPayload` |
| `deleteComment` | `slug: String!, id: ID!` | `DeletionStatus` |

### Pagination

All connections are Relay-style: `edges { cursor node { ... } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor }`. Pass `first`/`after` to page forward or `last`/`before` to page backward; exactly one of `first`/`last` is required. Cursors are the item's `createdAt` in epoch milliseconds.

### Example

```graphql
mutation {
  login(email: "jane@example.com", password: "secret") {
    user { username token }
  }
}

query {
  articles(first: 10, withTag: "dragons") {
    edges {
      cursor
      node { slug title favoritesCount author { username } }
    }
    pageInfo { hasNextPage endCursor }
  }
}
```

### Errors

* Failed `login` → GraphQL error with `errorType: UNAUTHENTICATED`.
* Bean-validation failures → GraphQL error with `errorType: BAD_REQUEST` and per-field messages in `extensions`; `createUser` instead returns them as an `Error { message, errors { key value } }` union member.
* Unknown slug/username → `ResourceNotFoundException` surfaced as a generic GraphQL error.
