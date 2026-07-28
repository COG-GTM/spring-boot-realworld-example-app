# Golden path: anatomy of an endpoint

The reference implementation is the one already in the repo: **`POST /articles/{slug}/comments`**.
Read it top to bottom before adding an endpoint of your own, then follow the same order and the
checklist at the bottom.

### 0. Contract first

`03-client-contract.md` + the RealWorld spec decide the shape: request `{"comment": {"body": …}}`,
response `201` with `{"comment": {…, "author": {…}}}`, `422` on an empty body, `404` for an unknown
slug, `401` without a token.

### 1. `api` — the controller method

`api/CommentsApi.java`

```java
@PostMapping
public ResponseEntity<?> createComment(
    @PathVariable("slug") String slug,
    @AuthenticationPrincipal User user,                       // current user, never read the header
    @Valid @RequestBody NewCommentParam newCommentParam) {    // validated, root-unwrapped payload
  Article article =
      articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);  // 404
  Comment comment = new Comment(newCommentParam.getBody(), user.getId(), article.getId());
  commentRepository.save(comment);                            // write through the core repository
  return ResponseEntity.status(201)
      .body(commentResponse(commentQueryService.findById(comment.getId(), user).get()));
}                                                             // ^ re-read via the query service
```

Five conventions in twelve lines: `@AuthenticationPrincipal`, `@Valid @RequestBody` param object,
`orElseThrow(ResourceNotFoundException::new)` instead of hand-built 404s, write through the `core`
repository, and **read back through the query service** so the response is a `*Data` DTO rather than
an entity.

Mutating endpoints that operate on an existing row add the ownership check between lookup and write
(`ArticleApi.updateArticle`, `CommentsApi.deleteComment`):

```java
if (!AuthorizationService.canWriteComment(user, article, comment)) {
  throw new NoAuthorizationException();                        // 403
}
```

### 2. `api` — the response wrapper

Bottom of the same class — one private helper per controller, reused by every method:

```java
private Map<String, Object> commentResponse(CommentData commentData) {
  return new HashMap<String, Object>() {
    {
      put("comment", commentData);
    }
  };
}
```

### 3. `application` — the request payload

`NewCommentParam` (package-private, bottom of `CommentsApi.java` because it is single-use; a shared
one would live in `application/comment/`):

```java
@Getter
@NoArgsConstructor
@JsonRootName("comment")
class NewCommentParam {
  @NotBlank(message = "can't be empty")
  private String body;
}
```

### 4. `core` — entity and repository interface

`core/comment/Comment.java` generates its own `id`/`createdAt` and exposes no setters;
`core/comment/CommentRepository.java` declares `save` / `findById` / `remove` in domain terms. State
changes are methods on the entity (`Article.update(...)`) — a service never reaches in and mutates
fields.

### 5. `infrastructure` — SQL

`CommentMapper` (`@Mapper`, `@Param`-named args) + `resources/mapper/CommentMapper.xml` with the
`<insert>` and a `<resultMap>` aliasing `body` → `commentBody`. `MyBatisCommentRepository` implements
the `core` interface and is the only place that touches the mapper.

### 6. `application` — the read model

`CommentQueryService.findById(id, user)` calls `CommentReadService` (a *read* `@Mapper` returning
`CommentData`) and enriches the projection with the user-relative `following` flag. Note the
asymmetry: `findByArticleId` null-checks `user` (the list endpoint is public), `findById` does not —
it is only reachable from authenticated endpoints, so pass it a non-null `user`.

### 7. `graphql` — parity

`CommentMutation.createComment` performs the same operation over DGS, calling the same entity and the
same repository. New write operations get a matching mutation or an explicit note in the PR saying
why not.

### 8. Tests

`api/CommentsApiTest.java`: `should_create_comment_success` asserts `201` **and** `comment.body`;
`should_get_422_with_empty_body` asserts `errors.body[0]`;
`should_get_403_if_not_author_of_article_or_author_of_comment_when_delete_comment` covers ownership.

---

## Checklist for a new endpoint

- [ ] Route added to the controller that owns that resource path (new controller only for a new path).
- [ ] Request payload is a `@JsonRootName` param object with `@NotBlank(message = "can't be empty")`-style validation.
- [ ] Lookups use `Optional` + `orElseThrow(ResourceNotFoundException::new)`.
- [ ] Ownership enforced via `AuthorizationService`, throwing `NoAuthorizationException`.
- [ ] Writes go through a `core` repository; entity state changes are methods on the entity.
- [ ] New SQL lives in the mapper XML, parameterised with `#{}`; schema changes are a new Flyway migration.
- [ ] Response built with the controller's private `xxxResponse(...)` helper; status code per the table in `02-rest-api-conventions.md`.
- [ ] GraphQL parity added, or its absence justified in the PR.
- [ ] `@WebMvcTest` covers success (JSON shape), auth failure, ownership failure, validation failure.
- [ ] `./gradlew spotlessJavaApply && ./gradlew test` clean; PR states whether the client contract changed.
