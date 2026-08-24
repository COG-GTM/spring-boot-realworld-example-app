# API reference

The backend exposes REST and GraphQL APIs on the same Spring Boot process.

## Authentication

Registration and login return a JWT in the `user.token` field. Send it on authenticated requests as:

```text
Authorization: Token <jwt>
```

REST security is route-based. GraphQL is served through a public HTTP route, and protected data fetchers and mutations read the authenticated user from Spring Security.

## REST API

The REST base URL is `http://localhost:8080`; routes do not use an `/api` prefix.

Request bodies use the RealWorld root objects:

```json
{
  "user": {
    "email": "reader@example.com",
    "password": "secret"
  }
}
```

```json
{
  "article": {
    "title": "A title",
    "description": "A short description",
    "body": "Article body",
    "tagList": ["java", "spring"]
  }
}
```

### Users

| Method | Path | Authentication | Behavior |
| --- | --- | --- | --- |
| `POST` | `/users` | Public | Register a user; returns `201` with `user` and JWT |
| `POST` | `/users/login` | Public | Log in; returns `200` with `user` and JWT |
| `GET` | `/user` | Required | Return the current user and the supplied JWT |
| `PUT` | `/user` | Required | Update supplied user fields |

### Profiles

| Method | Path | Authentication | Behavior |
| --- | --- | --- | --- |
| `GET` | `/profiles/{username}` | Optional | Return a profile; `following` is viewer-specific |
| `POST` | `/profiles/{username}/follow` | Required | Follow the profile |
| `DELETE` | `/profiles/{username}/follow` | Required | Unfollow the profile |

### Articles

| Method | Path | Authentication | Behavior |
| --- | --- | --- | --- |
| `GET` | `/articles` | Optional | List articles with offset pagination and filters |
| `GET` | `/articles/feed` | Required | List articles from followed users |
| `POST` | `/articles` | Required | Create an article |
| `GET` | `/articles/{slug}` | Optional | Return one article |
| `PUT` | `/articles/{slug}` | Required, author only | Update an article |
| `DELETE` | `/articles/{slug}` | Required, author only | Delete an article; returns `204` |
| `POST` | `/articles/{slug}/favorite` | Required | Favorite an article |
| `DELETE` | `/articles/{slug}/favorite` | Required | Unfavorite an article |

`GET /articles` accepts:

| Parameter | Default | Meaning |
| --- | --- | --- |
| `offset` | `0` | Zero-based result offset; negative values become `0` |
| `limit` | `20` | Page size; values above `100` are capped at `100` |
| `tag` | — | Filter by tag name |
| `author` | — | Filter by author username |
| `favorited` | — | Filter by username that favorited the article |

`GET /articles/feed` accepts `offset` and `limit` with the same defaults and limits.

### Comments and tags

| Method | Path | Authentication | Behavior |
| --- | --- | --- | --- |
| `GET` | `/articles/{slug}/comments` | Optional | List comments for an article |
| `POST` | `/articles/{slug}/comments` | Required | Add a comment; returns `201` |
| `DELETE` | `/articles/{slug}/comments/{id}` | Required, comment or article author | Delete a comment; returns `204` |
| `GET` | `/tags` | Public | Return all tag names |

### REST responses and errors

Single resources are wrapped as `user`, `profile`, `article`, or `comment`. Article lists return `articles` and `articlesCount`; comment and tag lists return `comments` and `tags`.

Expected error statuses include:

- `401 Unauthorized` for protected REST routes without a valid JWT
- `403 Forbidden` when an authenticated user cannot modify a resource
- `404 Not Found` when a requested user, article, comment, or relation is absent
- `422 Unprocessable Entity` for request validation or login failures

Validation errors use the RealWorld shape:

```json
{
  "errors": {
    "email": ["should be an email"]
  }
}
```

## GraphQL API

- Endpoint: `http://localhost:8080/graphql`
- GraphiQL: `http://localhost:8080/graphiql`
- Schema source: `src/main/resources/schema/schema.graphqls`

### Queries

| Field | Authentication | Result |
| --- | --- | --- |
| `article(slug)` | Optional | One article |
| `articles(...)` | Optional | Filtered article connection |
| `feed(...)` | Required | Current user's personalized article connection |
| `me` | Required for data | Current user |
| `profile(username)` | Optional | `ProfilePayload` |
| `tags` | Public | Tag names |

`articles` supports the filters `authoredBy`, `favoritedBy`, and `withTag`.

### Mutations

| Field | Authentication | Behavior |
| --- | --- | --- |
| `createUser(input)` | Public | Register a user |
| `login(email, password)` | Public | Log in |
| `updateUser(changes)` | Required | Update the current user |
| `followUser(username)` | Required | Follow a profile |
| `unfollowUser(username)` | Required | Unfollow a profile |
| `createArticle(input)` | Required | Create an article |
| `updateArticle(slug, changes)` | Required, author only | Update an article |
| `deleteArticle(slug)` | Required, author only | Delete an article |
| `favoriteArticle(slug)` | Required | Favorite an article |
| `unfavoriteArticle(slug)` | Required | Unfavorite an article |
| `addComment(slug, body)` | Required | Add a comment |
| `deleteComment(slug, id)` | Required, comment or article author | Delete a comment |

### Cursor pagination

Article connections and article comment connections accept:

- `first` and optional `after` for forward pagination
- `last` and optional `before` for backward pagination

The implementation requires `first` or `last`. Page sizes default to `20` when a non-positive value is supplied and are capped at `1000`. Cursors are millisecond timestamps encoded as decimal strings.

Example:

```graphql
query RecentJavaArticles {
  articles(first: 10, withTag: "java") {
    edges {
      cursor
      node {
        slug
        title
        author {
          username
        }
      }
    }
    pageInfo {
      endCursor
      hasNextPage
    }
  }
}
```

Authenticated request:

```bash
curl http://localhost:8080/graphql \
  -H 'Authorization: Token <jwt>' \
  -H 'Content-Type: application/json' \
  --data '{"query":"query { me { email username token } }"}'
```
