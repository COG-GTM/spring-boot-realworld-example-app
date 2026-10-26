---
name: springboot-2-to-3
description: Migrate a Spring Boot 2.7.x service to the latest 3.x (javax -> jakarta, Spring Security 6 SecurityFilterChain, springdoc 2, Hibernate 6, property renames) with OpenRewrite as the first pass and targeted manual fixes. Requires JDK 17 already in place.
---

# Spring Boot 2.x -> 3.x

Preconditions (stop and report `blocked` if not met): JDK is 17+, Boot is 2.7.x (if it is older,
bump to 2.7.18 first in its own PR), CLM is clean.

Target: the latest **3.5.x** GA. Do not go to 4.x here.

## Footprint
`pom.xml`, `src/main/**/*.java`, `src/test/**/*.java`, `src/main/resources/application*.yml`,
`README.md`. No pipeline/JDK changes.

## Steps
1. Create branch `platform/04-spring-boot-3`.
2. Mechanical pass (commit on its own as `PLAT-UPG feature: openrewrite Spring Boot 3 recipes`):
   ```bash
   mvn -B -ntp org.openrewrite.maven:rewrite-maven-plugin:run \
     -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:RELEASE \
     -Drewrite.activeRecipes=org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5
   ```
   If OpenRewrite is unavailable (no network / plugin resolution fails), do the same by hand:
   parent `3.5.x`, `javax.{persistence,validation,servlet,annotation}` -> `jakarta.*`.
3. Manual pass (second commit). Check each:
   - **Security**: `WebSecurityConfigurerAdapter` -> `@Bean SecurityFilterChain`;
     `antMatchers` -> `requestMatchers`; `.and()` chains -> lambda DSL
     (`http.csrf(c -> c.disable())`). Custom filters keep working; re-register with
     `addFilterBefore`. `hasRole` semantics unchanged.
   - **springdoc**: `springdoc-openapi-ui` 1.x -> `springdoc-openapi-starter-webmvc-ui` 2.x;
     property prefixes unchanged.
   - **Config props**: delete `@ConstructorBinding` on `@ConfigurationProperties` classes with a
     single constructor (now implicit). Rename `spring.redis.*` -> `spring.data.redis.*`,
     `spring.jpa.hibernate.use-new-id-generator-mappings` (remove), `server.max-http-header-size`
     -> `server.max-http-request-header-size`.
   - **Actuator**: `/actuator/prometheus` needs `micrometer-registry-prometheus` explicitly.
   - **Hibernate 6**: `@Type` string names removed; ID generation `GenerationType.AUTO` now uses
     sequences — if the schema is managed externally, set
     `spring.jpa.hibernate.id.new_generator_mappings` appropriately or use `IDENTITY`.
     Numeric literal/BigDecimal scale changes in JPQL; rerun repository tests.
   - **Tests**: `@MockBean` still exists in 3.x. MockMvc security matchers unchanged.
   - **Dependencies you overrode for CLM** (`snakeyaml.version`, `tomcat.version`,
     `spring-framework.version`): remove the overrides now that the Boot 3 BOM manages fixed
     versions — unless the report still demands newer.
   - **Trailing-slash matching** is off by default in Spring MVC 6; check tests hitting `/path/`.
4. Validate: `mvn -B -ntp clean verify`, boot the jar, curl `/actuator/health` and one secured
   endpoint with a valid credential (expect 200) and no credential (expect 401).
5. README: Runtime row -> `Java 17, Spring Boot 3.5`. Commit and open PR
   `[platform 4/N] Spring Boot 3.5` with sections: mechanical changes, manual changes, behaviour
   changes reviewers should know (trailing slash, id generation).

## Done when
Parent is 3.5.x, no `javax.` imports remain (`grep -r "import javax\." src || true` is empty, except
`javax.crypto`/`javax.sql` which are JDK packages), build green, app boots, PR open.
