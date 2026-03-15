## REST API

Base URL: `http://localhost:8080`

All endpoints return JSON. Authenticated endpoints expect `Authorization: Token <jwt>`.

### Articles

- GET `/articles`
  - Query: `offset` (int, default 0), `limit` (int, default 20), `tag`, `favorited`, `author`
  - Response: `articles` (Article[]), `articlesCount` (int)
  - Example:
    ```bash
    curl 'http://localhost:8080/articles?tag=java&limit=10'
    ```

- GET `/articles/feed` (auth)
  - Query: `offset`, `limit`
  - Response: `articles`, `articlesCount`

- POST `/articles` (auth)
  - Body (JSON root: `article`):
    ```json
    {
      "article": {
        "title": "Intro to Spring",
        "description": "Basics",
        "body": "...",
        "tagList": ["spring","java"]
      }
    }
    ```
  - Response: `{ "article": Article }`

- GET `/articles/{slug}`
  - Response: `{ "article": Article }`

- PUT `/articles/{slug}` (auth, author only)
  - Body (JSON root: `article`): any of `title`, `description`, `body`
    ```json
    { "article": { "title": "Updated title" } }
    ```
  - Response: `{ "article": Article }`

- DELETE `/articles/{slug}` (auth, author only)
  - Response: 204 No Content

### Article Favorites

- POST `/articles/{slug}/favorite` (auth)
  - Response: `{ "article": Article }`

- DELETE `/articles/{slug}/favorite` (auth)
  - Response: `{ "article": Article }`

### Comments

- GET `/articles/{slug}/comments`
  - Response: `{ "comments": Comment[] }`

- POST `/articles/{slug}/comments` (auth)
  - Body (JSON root: `comment`):
    ```json
    { "comment": { "body": "Great post!" } }
    ```
  - Response: 201 `{ "comment": Comment }`

- DELETE `/articles/{slug}/comments/{id}` (auth, author of comment)
  - Response: 204 No Content

### Profiles

- GET `/profiles/{username}` (auth optional)
  - Response: `{ "profile": Profile }`

- POST `/profiles/{username}/follow` (auth)
  - Response: `{ "profile": Profile }`

- DELETE `/profiles/{username}/follow` (auth)
  - Response: `{ "profile": Profile }`

### Tags

- GET `/tags`
  - Response: `{ "tags": string[] }`

### Users and Auth

- POST `/users`
  - Body (JSON root: `user`): `email`, `username`, `password`
  - Response: `{ "user": UserWithToken }`

- POST `/users/login`
  - Body (JSON root: `user`): `email`, `password`
  - Response: `{ "user": UserWithToken }`

- GET `/user` (auth)
  - Response: `{ "user": UserWithToken }`

- PUT `/user` (auth)
  - Body (JSON root: `user`): any of `email`, `username`, `password`, `image`, `bio`
  - Response: `{ "user": UserWithToken }`

### Data Models

- Article
  - `slug`, `title`, `description`, `body`, `tagList` (string[]), `createdAt`, `updatedAt`, `favorited` (bool), `favoritesCount` (int), `author` (Profile)

- Comment
  - `id`, `body`, `createdAt`, `updatedAt`, `author` (Profile)

- Profile
  - `username`, `bio`, `image`, `following` (bool)

- UserWithToken
  - `email`, `username`, `bio`, `image`, `token`

