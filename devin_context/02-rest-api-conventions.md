# REST API conventions

Reference implementations: `api/ArticleApi.java`, `api/CommentsApi.java`, `api/UsersApi.java`.

## Controllers

```java
@RestController
@RequestMapping(path = "/articles/{slug}/comments")   // one controller per resource path
@AllArgsConstructor                                   // constructor injection, no @Autowired
public class CommentsApi {
  private ArticleRepository articleRepository;        // package-private fields, no `final`, no Javadoc
  private CommentQueryService commentQueryService;
```

- One controller per resource path; nested resources get their own class
  (`ArticlesApi` = collection, `ArticleApi` = `/articles/{slug}`, `CommentsApi` = comments).
- Method-level `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` with no path when the
  class-level `@RequestMapping` already covers it; add only the sub-path (`@PutMapping(path = "{id}")`).
- Return `ResponseEntity<?>`; build the body with the private `xxxResponse(...)` helper at the bottom
  of the controller (see below). Controllers hold no business logic and no SQL.
- No `/api` prefix — the app is mounted at the root (`http://localhost:8080/articles`).

## Request payloads

- One `*Param` class per operation in `application/<aggregate>/`, or a package-private class at the
  bottom of the controller file when it is single-use (`NewCommentParam` in `CommentsApi.java`,
  `LoginParam` in `UsersApi.java`).
- Always `@Getter @NoArgsConstructor @JsonRootName("<root>")`; add `@AllArgsConstructor @Builder`
  when tests construct it (`NewArticleParam`).
- Validate with Bean Validation and the project's message style — lowercase, no trailing period:
  `@NotBlank(message = "can't be empty")`, `@Email(message = "should be an email")`.
- Cross-aggregate validation is a custom constraint + validator pair in the same package
  (`DuplicatedArticleConstraint` / `DuplicatedArticleValidator`).
- Bind with `@Valid @RequestBody`. Root unwrapping is global
  (`spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true`), so the param class maps the *inner*
  object, and `@JsonRootName` names the wrapper.

## Responses

Every successful body is a single root-wrapped object, produced by a private helper:

```java
private Map<String, Object> commentResponse(CommentData commentData) {
  return new HashMap<String, Object>() {
    {
      put("comment", commentData);
    }
  };
}
```

| Case | Status | Body |
| --- | --- | --- |
| Read / update | `200` | `{"<root>": {...}}` |
| Create | `201` (`ResponseEntity.status(201)`) | `{"<root>": {...}}` |
| Delete | `204` (`ResponseEntity.noContent().build()`) | empty |
| Validation failure | `422` | `{"errors": {"<field>": ["message"]}}` |
| Unauthenticated | `401` | empty (`HttpStatusEntryPoint`) |
| Not the owner | `403` | throw `NoAuthorizationException` |
| Missing resource | `404` | throw `ResourceNotFoundException` |

Serialize DTOs (`application/data/*Data`), never `core` entities.

## Errors

Never build an error body by hand. Throw the exception; `CustomizeExceptionHandler`
(`@RestControllerAdvice`) renders it:

| Throw | Renders |
| --- | --- |
| `ResourceNotFoundException` | `404` |
| `NoAuthorizationException` | `403` |
| `InvalidRequestException(errors)` | `422` + field errors |
| `InvalidAuthenticationException` | `422` + `{"message": ...}` |
| Bean Validation failure | `422` + field errors (handled automatically) |

If you need a new error semantic, add an exception in `api/exception/` with `@ResponseStatus` and, if
its body is non-standard, a handler in `CustomizeExceptionHandler`.

## Auth & authorization

- The current user is injected: `@AuthenticationPrincipal User user`. It is `null` for anonymous
  requests on public endpoints — handle that instead of NPE-ing (`CommentQueryService.findByArticleId`).
- `JwtTokenFilter` reads `Authorization: Token <jwt>` and populates the security context.
- New routes are **authenticated by default** (`anyRequest().authenticated()` in `WebSecurityConfig`).
  Making a route public is an explicit `antMatchers(...).permitAll()` change and must be called out in
  the PR description.
- Ownership checks use `AuthorizationService.canWriteArticle` / `canWriteComment` and throw
  `NoAuthorizationException`; never inline `user.getId().equals(...)` in a controller.
