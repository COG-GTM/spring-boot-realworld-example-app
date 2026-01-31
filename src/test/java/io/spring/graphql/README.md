# GraphQL API Tests

This directory contains comprehensive test coverage for the GraphQL API layer of the Spring Boot RealWorld Example App.

## Test Structure

The GraphQL tests are organized to mirror the structure of the GraphQL implementation:

### Base Test Classes
- **GraphQLTestBase.java**: Base class providing common test fixtures and authentication utilities for GraphQL tests
- **GraphQLIntegrationTestBase.java**: Base class for integration tests using `@SpringBootTest`

### Mutation Tests
- **UserMutationTest.java**: Tests for user-related GraphQL mutations
  - User registration (`createUser`)
  - User login (`login`)
  - User profile updates (`updateUser`)
  - Authentication error handling

- **ArticleMutationTest.java**: Tests for article-related GraphQL mutations
  - Article creation (`createArticle`)
  - Article updates (`updateArticle`)
  - Article favoriting (`favoriteArticle`, `unfavoriteArticle`)
  - Article deletion (`deleteArticle`)
  - Authorization checks

- **CommentMutationTest.java**: Tests for comment-related GraphQL mutations
  - Adding comments to articles (`addComment`)
  - Deleting comments (`deleteComment`)
  - Authorization for comment operations

### Query Tests
- **ArticleQueryTest.java**: Tests for article-related GraphQL queries
  - Fetching single articles by slug (`article`)
  - Fetching multiple articles with pagination (`articles`)
  - Filtering articles by tag, author, or favorited user
  - User feed queries (`feed`)

- **ProfileQueryTest.java**: Tests for user profile queries
  - Fetching user profiles by username (`profile`)
  - Following status in profiles

### Data Fetcher Tests
- **ArticleDatafetcherTest.java**: Tests for GraphQL data fetchers
  - Article author resolution
  - Article comments resolution
  - Pagination information
  - Favorited article data

### Error Handling Tests
- **GraphQLErrorHandlingTest.java**: Tests for GraphQL error handling
  - Validation errors
  - Resource not found errors
  - Authentication errors
  - Authorization errors
  - Invalid query parameter handling

## Running the Tests

### Run all GraphQL tests:
```bash
./gradlew test --tests "io.spring.graphql.*"
```

### Run specific test class:
```bash
./gradlew test --tests "io.spring.graphql.UserMutationTest"
```

### Run specific test method:
```bash
./gradlew test --tests "io.spring.graphql.UserMutationTest.should_create_user_successfully"
```

## Test Coverage

The GraphQL tests provide comprehensive coverage for:

1. **Mutations**: All GraphQL mutations are tested including:
   - User registration and authentication
   - Article CRUD operations
   - Comment CRUD operations
   - Follow/unfollow operations
   - Favorite/unfavorite operations

2. **Queries**: All GraphQL queries are tested including:
   - Single resource queries (article, profile)
   - Collection queries with pagination (articles, feed)
   - Filtering and sorting

3. **Data Fetchers**: Tests verify that:
   - Nested data is properly resolved (e.g., article authors, comments)
   - Pagination cursors work correctly
   - Context data is properly passed between resolvers

4. **Error Handling**: Tests verify that:
   - Validation errors are properly returned
   - Authentication/authorization is enforced
   - Resource not found errors are handled
   - Invalid parameters are rejected

## Testing Approach

The tests use the following technologies and patterns:

- **DGS Framework**: Netflix DGS (Domain Graph Service) framework for GraphQL
- **DgsQueryExecutor**: For executing GraphQL queries and mutations in tests
- **Spring Boot Test**: For integration testing with Spring context
- **Mockito**: For mocking dependencies
- **JUnit 5**: For test execution and assertions
- **AssertJ**: For fluent assertions

## Test Patterns

### Authentication in Tests
Tests that require authentication use the `authenticateUser()` method from the base class:

```java
@Test
public void should_create_article_successfully() {
    authenticateUser();
    // ... test code ...
    clearAuthentication();
}
```

### GraphQL Query Execution
Tests execute GraphQL queries using the `DgsQueryExecutor`:

```java
String query = "query GetArticle($slug: String!) { article(slug: $slug) { title } }";
Map<String, Object> variables = new HashMap<>();
variables.put("slug", "test-article");

Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
    query, "data.article", variables, new TypeRef<Map<String, Object>>() {});
```

### Mocking Dependencies
Tests mock repository and service dependencies:

```java
@MockBean private ArticleRepository articleRepository;

when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
```

## Notes

- These tests follow the same patterns as the existing REST API tests in `src/test/java/io/spring/api/`
- The tests use the same domain models and services as the REST API
- GraphQL-specific error handling is tested separately from business logic errors
- Pagination tests verify both cursor-based pagination and page info

## Future Enhancements

Potential areas for additional test coverage:

1. Subscription tests (if GraphQL subscriptions are added)
2. Performance tests for complex nested queries
3. Schema validation tests
4. Custom scalar type tests
5. Directive tests (if custom directives are added)
