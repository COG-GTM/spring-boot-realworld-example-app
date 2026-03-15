## Models (DTOs)

This project exposes DTOs in REST responses and GraphQL types.

### Article
- Fields: `slug`, `title`, `description`, `body`, `tagList: string[]`, `favorited: boolean`, `favoritesCount: number`, `createdAt`, `updatedAt`, `author: Profile`

### Comment
- Fields: `id`, `body`, `createdAt`, `updatedAt`, `author: Profile`

### Profile
- Fields: `username`, `bio`, `image`, `following: boolean`

### UserWithToken
- Fields: `email`, `username`, `bio`, `image`, `token`

### REST Request Payloads

- `NewArticleParam` (root key `article`): `title` (required), `description` (required), `body` (required), `tagList`
- `UpdateArticleParam` (root key `article`): any of `title`, `description`, `body`
- `RegisterParam` (root key `user`): `email` (required), `username` (required), `password` (required)
- `LoginParam` (root key `user`): `email` (required), `password` (required)
- `UpdateUserParam` (root key `user`): any of `email`, `username`, `password`, `bio`, `image`

