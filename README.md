# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# *NEW* GraphQL Support  

Following some DDD principles. REST or GraphQL is just a kind of adapter. And the domain layer will be consistent all the time. So this repository implement GraphQL and REST at the same time.

The GraphQL schema is https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/src/main/resources/schema/schema.graphqls and the visualization looks like below.

![](graphql-schema.png)

And this implementation is using [dgs-framework](https://github.com/Netflix/dgs-framework) which is a quite new java graphql server framework.
# How it works

The application uses Spring Boot (Web, Mybatis).

* Use the idea of Domain Driven Design to separate the business term and infrastructure term.
* Use MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* Use [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern to separate the read model and write model.

And the code is organized as this:

1. `api` is the web layer implemented by Spring MVC
2. `core` is the business model including entities and services
3. `application` is the high-level services for querying the data transfer objects
4. `infrastructure`  contains all the implementation classes as the technique details

# Security

Integration with Spring Security and add other filter for jwt token process.

The secret key is stored in `application.properties`.

# Database

It uses a ~~H2 in-memory database~~ sqlite database (for easy local test without losing test data after every restart), can be changed easily in the `application.properties` for any other database.

# Article search

`GET /articles/search?q=<text>&offset=0&limit=20` performs full-text search over article
title, description and body. It is public (a token is optional; when present, `favorited` and
`following` are filled in for the current user) and returns the same shape as `GET /articles`:
`{"articles": [...], "articlesCount": <total matches>}`. `offset`/`limit` behave like the other
list endpoints (`limit` is capped at 100). A missing `q` returns `400`; a `q` with no searchable
terms returns an empty list.

How it works:

* **Index**: an SQLite [FTS5](https://www.sqlite.org/fts5.html) virtual table `articles_fts`
  (migration `V2__create_article_search_index.sql`), kept in sync with `articles` by
  insert/update/delete triggers. `article_search_ids` maps each article id to a stable FTS rowid so
  the triggers update the index by rowid rather than scanning it. It uses the `porter unicode61 remove_diacritics 2` tokenizer, so
  matching is case-insensitive, accent-insensitive and stemmed (`mocks` matches `mock`).
* **Query parsing**: the first 256 characters of the input are split into letter/mark/digit terms
  (at most 10); FTS5 syntax characters are discarded so user input can never change the query structure. Each term becomes a
  prefix match (`"term"*`) and all terms must match (AND).
* **Ranking**: [BM25](https://en.wikipedia.org/wiki/Okapi_BM25) via FTS5's `bm25()` with column
  weights **title = 10, description = 5, body = 1**, so a hit in the title outranks one in the
  description, which outranks one in the body; within a column, rarer terms and shorter fields
  score higher. Equal scores are ordered by `created_at` (newest first) and then by id, which
  keeps offset pagination stable.

# Getting started

You'll need Java 11 installed.

    ./gradlew bootRun

To test that it works, open a browser tab at http://localhost:8080/tags .  
Alternatively, you can run

    curl http://localhost:8080/tags

# Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.
	
    ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
    docker run -p 8081:8080 spring-boot-realworld-example-app

# Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

# Run test

The repository contains a lot of test cases to cover both api test and repository test.

    ./gradlew test

# Code format

Use spotless for code format.

    ./gradlew spotlessJavaApply

# Help

Please fork and PR to improve the project.
