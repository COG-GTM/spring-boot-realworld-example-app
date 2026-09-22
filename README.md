Spring Boot 2.6 (Java 11) backend implementing the RealWorld "Conduit" spec, exposing both a REST API and a GraphQL API (Netflix DGS), with MyBatis + SQLite persistence and JWT auth.
Run with ./gradlew bootRun, then hit http://localhost:8080/tags (REST) or http://localhost:8080/graphiql (GraphQL); tests: ./gradlew test.
