## GraphQL API

Endpoint: `POST http://localhost:8080/graphql`

Send GraphQL queries and mutations with JSON body: `{ "query": "...", "variables": { ... } }`.

### Schema Overview

Key operations (see `src/main/resources/schema/schema.graphqls`):

- Query
  - `article(slug: String!): Article`
  - `articles(first, after, last, before, authoredBy, favoritedBy, withTag): ArticlesConnection`
  - `me: User`
  - `feed(first, after, last, before): ArticlesConnection`
  - `profile(username: String!): ProfilePayload`
  - `tags: [String]`

- Mutation
  - `createUser(input: CreateUserInput): UserResult`
  - `login(email: String!, password: String!): UserPayload`
  - `updateUser(changes: UpdateUserInput!): UserPayload`
  - `followUser(username: String!): ProfilePayload`
  - `unfollowUser(username: String!): ProfilePayload`
  - `createArticle(input: CreateArticleInput!): ArticlePayload`
  - `updateArticle(slug: String!, changes: UpdateArticleInput!): ArticlePayload`
  - `favoriteArticle(slug: String!): ArticlePayload`
  - `unfavoriteArticle(slug: String!): ArticlePayload`
  - `deleteArticle(slug: String!): DeletionStatus`
  - `addComment(slug: String!, body: String!): CommentPayload`
  - `deleteComment(slug: String!, id: ID!): DeletionStatus`

### Examples

- Fetch articles with pagination
  ```graphql
  query Articles($first: Int, $after: String) {
    articles(first: $first, after: $after) {
      edges { cursor node { slug title author { username } } }
      pageInfo { hasNextPage endCursor }
    }
  }
  ```

- Current user
  ```graphql
  query { me { username email token profile { image bio } } }
  ```

- Create article
  ```graphql
  mutation Create($input: CreateArticleInput!) {
    createArticle(input: $input) { article { slug title } }
  }
  ```
  Variables:
  ```json
  {"input": {"title": "Intro", "description": "Basics", "body": "...", "tagList": ["spring"]}}
  ```

- Add comment
  ```graphql
  mutation($slug: String!, $body: String!) {
    addComment(slug: $slug, body: $body) { comment { id body author { username } } }
  }
  ```

### Authentication

Add header: `Authorization: Bearer <jwt>` when required.

### Connections and Cursors

Pagination uses Relay-style connections: `edges { cursor node }` + `pageInfo { ... }`.

