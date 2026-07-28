# GraphQL conventions (Netflix DGS)

GraphQL is a second **adapter** over the same domain. REST and GraphQL must never diverge in
behaviour — only in transport.

## Schema first

- The schema is `src/main/resources/schema/schema.graphqls`. Edit it first.
- `com.netflix.dgs.codegen` regenerates `io.spring.graphql.types.*` and `DgsConstants` into
  `build/generated` on `./gradlew build` — never hand-write or commit those types.
- Reference `@DgsData` fields through the generated constants
  (`@DgsData(parentType = MUTATION.TYPE_NAME, field = MUTATION.AddComment)`), never string literals.

## Components

- Mutations live in `graphql/<Aggregate>Mutation.java`, queries/field resolvers in
  `graphql/<Aggregate>Datafetcher.java`; both are `@DgsComponent @AllArgsConstructor`.
- Arguments are bound with `@InputArgument("name")`.
- Return `DataFetcherResult<XPayload>` and pass the DTO through `.localContext(...)` so child
  resolvers can read it, with an empty generated payload as `.data(...)` — copy
  `CommentMutation.createComment`.
- Pagination uses the cursor helpers (`CursorPager`, `CursorPageParameter`, `PageCursor`) and
  returns Relay-style `*Connection` types.

## Auth & errors

- There is no `@AuthenticationPrincipal` here: use
  `SecurityUtil.getCurrentUser().orElseThrow(AuthenticationException::new)`.
- Reuse the same domain exceptions as REST (`ResourceNotFoundException`, `NoAuthorizationException`)
  and the same `AuthorizationService` checks; `GraphQLCustomizeExceptionHandler` maps them to
  GraphQL errors.

## Parity rule

Adding a write operation to REST? Add the matching mutation (or state in the PR description why it
is REST-only). Both adapters must call the *same* command/query services — no duplicated logic.
