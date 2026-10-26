---
name: jdk-upgrade
description: Upgrade a Maven/Spring Boot service's JDK (e.g. 11 -> 17) across pom.xml, .java-version, pipeline manifest and CI, fixing compile/runtime fallout, without changing the Spring Boot version. Use when a service runs an older LTS than the GS baseline (17).
---

# JDK upgrade

Default target: **17** (GS baseline). Only go to 21 if the orchestrator asks. Do not change the
Spring Boot version in this playbook; Boot 2.7.x runs on 17, Boot 3.x requires 17.

## Footprint
`pom.xml` (`java.version`, `maven.compiler.*`, plugin versions needed for the new JDK),
`.java-version`, `.gs/pipeline.yml` or `Jenkinsfile` (`jdk`), `.github/workflows/ci.yml`,
Java sources only where the new JDK breaks compilation or tests, `README.md` runtime row.

## Steps
1. Record current JDK from `.java-version` / `pom.xml`. Confirm the target JDK is installed
   (`ls /usr/lib/jvm`, `sdk list java`) or install Temurin.
2. Create branch `platform/03-jdk-<target>`.
3. Edit `pom.xml`: `<java.version>`, `<maven.compiler.source/target>` (or `<maven.compiler.release>`).
   If the Boot parent is 2.x, make sure `maven-surefire-plugin` >= 2.22.2 and
   `maven-compiler-plugin` >= 3.10 (Boot 2.7 parent already does).
4. Update `.java-version`, the pipeline `jdk:` field and CI so every place agrees on the target.
5. Build with the target JDK (`JAVA_HOME=<target> mvn -B -ntp clean verify`). Typical fallout:
   - `--add-opens` needed for reflective libraries (Mockito/Byte Buddy on very old versions:
     bump them instead).
   - Removed modules: `javax.xml.bind` (add `jakarta.xml.bind-api` + `jaxb-runtime`),
     `javax.annotation` (`jakarta.annotation-api`), Nashorn.
   - Lombok / annotation processors: bump to a JDK-compatible version.
   - Strong encapsulation errors `InaccessibleObjectException`: fix the library version, never
     add blanket `--add-opens` to production config.
6. Boot the jar once (`java -jar target/*.jar`) and curl `/actuator/health`; stop it.
7. Update README runtime row. Commit `PLAT-UPG feature: upgrade runtime to JDK <target>` and open
   PR `[platform 3/N] JDK <target>`.

## Done when
All JDK references agree, `mvn verify` under the target JDK is green, app boots, PR open.
