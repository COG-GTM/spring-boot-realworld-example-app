# Architecture & layering

DDD-flavoured layering with CQRS. Four packages under `src/main/java/io/spring`:

```
api/            REST adapter (Spring MVC controllers, filters, exception handling)
graphql/        GraphQL adapter (Netflix DGS data fetchers + mutations)
application/    read model: query services + `data` DTOs, command params, validators
core/           write model: entities, repository interfaces, domain services
infrastructure/ technical detail: MyBatis mappers, repository impls, JWT service
```

## Dependency rules

- `api` and `graphql` are **adapters**. They may depend on `application` and `core`; they must not
  contain business rules and must not talk to MyBatis mappers directly.
- `application` may depend on `core` and on `infrastructure.mybatis.readservice` (read model only).
- `core` depends on **nothing** outside `core` (plus Lombok/Joda). No Spring annotations, no SQL.
- `infrastructure` implements `core` interfaces; nothing depends on it except Spring wiring.

Adding a dependency that points the other way is a rejected review comment, not a judgement call.

## CQRS: two paths through the app

**Writes** — mutate a `core` entity, persist through a `core` repository interface:

```
ArticleApi → ArticleCommandService → Article (core) → ArticleRepository (core interface)
                                                    → MyBatisArticleRepository (infrastructure)
```

**Reads** — never load an entity to render a response; project straight to a `*Data` DTO:

```
ArticleApi → ArticleQueryService (application) → ArticleReadService (MyBatis @Mapper) → ArticleData
```

A controller that has just written something re-reads it through the query service before
responding (see `CommentsApi.createComment`, `ArticleApi.updateArticle`). Do the same.

## Entities (`core`)

- Constructors set `id = UUID.randomUUID().toString()` and timestamps; no setters.
- Behaviour lives on the entity (`Article.update(...)`, `User.update(...)`), not in a service.
- `@Getter @NoArgsConstructor @EqualsAndHashCode(of = "id")` — MyBatis needs the no-arg constructor.
- Domain rules that span entities go in a `core.service` static helper, e.g. `AuthorizationService`.

## Application services

- `@Service @AllArgsConstructor`, constructor-injected fields, no `@Autowired` on fields.
- Command services are `@Validated` and validate their params with `@Valid` (`ArticleCommandService`).
- Query services return `Optional<T>` for single lookups, `List<T>` for collections, and
  `CursorPager<T>` for cursor pagination (`CommentQueryService`).
- DTOs live in `application/data`, are `@Data @NoArgsConstructor @AllArgsConstructor`, and implement
  `Node` when they can be paginated by cursor.

## Where new code goes — quick table

| You are adding | Put it in |
| --- | --- |
| A new HTTP route | `api/<Resource>Api.java` (one controller per resource path) |
| Request payload + validation | `application/<aggregate>/<Verb><Aggregate>Param.java` |
| A state change on an entity | a method on the entity in `core/<aggregate>/` |
| A new read/projection | `application/<Aggregate>QueryService` + `infrastructure/mybatis/readservice` |
| Raw SQL | `src/main/resources/mapper/<Mapper>.xml` |
| A schema change | a new `src/main/resources/db/migration/V<n>__<name>.sql` |
