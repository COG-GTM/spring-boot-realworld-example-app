## Pagination

This backend supports two pagination styles.

### REST (offset/limit)

- Endpoints: `GET /articles`, `GET /articles/feed`
- Query params: `offset` (default 0), `limit` (default 20)
- Response includes:
  - `articles`: array of items
  - `articlesCount`: total count

### GraphQL (cursor-based)

- Connections return:
  - `edges { cursor node { ... } }`
  - `pageInfo { hasNextPage, hasPreviousPage, startCursor, endCursor }`
- Queries accept: `first`, `after`, `last`, `before`

