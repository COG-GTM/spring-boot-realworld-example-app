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
## REST / GraphQL parity

Both adapters sit on the same application/domain layer. The table below lists every operation and where it is available (GraphQL at `POST /graphql`, schema in `src/main/resources/schema/schema.graphqls`).

| Area | Operation | REST | GraphQL | Parity |
|---|---|---|---|---|
| User | Register | `POST /users` | `createUser` mutation | Both (GraphQL returns validation errors as the `Error` member of `UserResult`) |
| User | Login | `POST /users/login` | `login` mutation | Both |
| User | Get current user | `GET /user` | `me` query | Both |
| User | Update current user | `PUT /user` | `updateUser` mutation | Both |
| Profile | Get profile | `GET /profiles/{username}` | `profile` query | Both |
| Profile | Follow user | `POST /profiles/{username}/follow` | `followUser` mutation | Both |
| Profile | Unfollow user | `DELETE /profiles/{username}/follow` | `unfollowUser` mutation | Both |
| Article | List articles (filter by tag / author / favorited) | `GET /articles?tag=&author=&favorited=` | `articles(withTag, authoredBy, favoritedBy)` query | Both |
| Article | List a profile's articles / favorites | `GET /articles?author=` / `?favorited=` | `Profile.articles` / `Profile.favorites` | Both (different shape) |
| Article | Feed of followed authors | `GET /articles/feed` | `feed` query | Both |
| Article | Total count of matching articles | `articlesCount` in list responses | `ArticlesConnection.totalCount` | Both (GraphQL added on this branch) |
| Article | Get article | `GET /articles/{slug}` | `article(slug)` query | Both |
| Article | Create article | `POST /articles` | `createArticle` mutation | Both |
| Article | Update article | `PUT /articles/{slug}` | `updateArticle` mutation | Both (neither can change `tagList`) |
| Article | Delete article | `DELETE /articles/{slug}` | `deleteArticle` mutation | Both |
| Favorite | Favorite article | `POST /articles/{slug}/favorite` | `favoriteArticle` mutation | Both |
| Favorite | Unfavorite article | `DELETE /articles/{slug}/favorite` | `unfavoriteArticle` mutation | Both |
| Comment | Add comment | `POST /articles/{slug}/comments` | `addComment` mutation | Both |
| Comment | List comments | `GET /articles/{slug}/comments` | `Article.comments` | Both (only GraphQL is paginated; REST returns all) |
| Comment | Delete comment | `DELETE /articles/{slug}/comments/{id}` | `deleteComment` mutation | Both |
| Tag | List tags | `GET /tags` | `tags` query | Both |
| Article | Another user's feed | – | `Profile.feed` | GraphQL only |
| Comment | Navigate from comment to its article | – | `Comment.article` | GraphQL only (graph traversal) |
| Pagination | Offset / limit | `offset`, `limit` params | – | REST only |
| Pagination | Cursor (`first`/`after`/`last`/`before`, `pageInfo`) | – | All `*Connection` fields | GraphQL only |

`totalCount` is only computed when it is selected, so existing GraphQL queries do not pay for the extra `COUNT` query.

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
