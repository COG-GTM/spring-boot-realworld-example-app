# Persistence conventions (MyBatis + Flyway)

Two distinct kinds of MyBatis interface — pick the right one:

| | Write model | Read model |
| --- | --- | --- |
| Interface | `infrastructure/mybatis/mapper/<Aggregate>Mapper.java` | `infrastructure/mybatis/readservice/<Aggregate>ReadService.java` |
| Returns | `core` entities | `application/data/*Data` DTOs |
| Called by | `MyBatis<Aggregate>Repository` (implements the `core` interface) | `application` query services |

Both are annotated `@Mapper`, take `@Param("...")`-named arguments, and are backed by an XML file in
`src/main/resources/mapper/<same-name>.xml` whose `namespace` is the fully-qualified interface name
(`mybatis.mapper-locations=mapper/*.xml`).

## SQL style

- SQL lives in XML, never in annotations and never in Java strings.
- Alias every column to its camelCase DTO/entity property (`C.created_at commentCreatedAt`) and map
  it in a `<resultMap>`; shared column lists go in a reusable `<sql id="...">` and are pulled in with
  `<include refid="..."/>` (see `CommentReadService.xml` including
  `ArticleReadService.profileColumns`).
- Shared `resultMap`s for DTOs live in `TransferData.xml` under the `transfer.data` namespace —
  reuse them (`resultMap="transfer.data.commentData"`) rather than redefining.
- Dynamic SQL uses `<where>` / `<if>`; cursor pagination follows the `page.direction.name() == "NEXT"`
  pattern and fetches `limit + 1` rows to compute `hasExtra`.
- Parameterise everything with `#{...}`. `${...}` is string interpolation — do not use it.

## Repositories

- The interface is in `core/<aggregate>/<Aggregate>Repository.java` and speaks the domain
  (`save`, `findBySlug`, `remove`); the implementation is
  `infrastructure/repository/MyBatis<Aggregate>Repository.java`, a Spring bean
  (`@Repository`, or `@Component` as in `MyBatisCommentRepository`) with constructor injection.
- `save` is an upsert from the caller's point of view: check existence, then `insert` or `update`
  (see `MyBatisArticleRepository.save`). Multi-statement writes are `@Transactional`.

## Schema changes

- Flyway migrations in `src/main/resources/db/migration/V<n>__<snake_case_name>.sql`. Never edit an
  applied migration; add a new one.
- `snake_case` table and column names; `id` is a UUID `varchar` primary key; timestamp columns are
  `created_at` / `updated_at`.
- Joda `DateTime` ↔ SQLite conversion is handled by `DateTimeHandler`
  (`mybatis.type-handlers-package=io.spring.infrastructure.mybatis`); do not convert dates in SQL.
- Local dev/test DB is SQLite (`dev.db`, deleted by `./gradlew clean`). Keep SQL portable: no
  vendor-specific syntax.
