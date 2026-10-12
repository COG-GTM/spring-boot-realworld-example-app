# syntax=docker/dockerfile:1

FROM --platform=$BUILDPLATFORM eclipse-temurin:11-jdk AS build
ARG MAVEN_MIRROR=
ENV MAVEN_MIRROR=${MAVEN_MIRROR}
WORKDIR /app
COPY gradlew build.gradle ./
COPY gradle gradle
COPY docker/maven-mirror.init.gradle /tmp/maven-mirror.init.gradle
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon -I /tmp/maven-mirror.init.gradle dependencies > /dev/null
COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon -I /tmp/maven-mirror.init.gradle bootJar -x test \
    && cp build/libs/*-SNAPSHOT.jar /app/app.jar

# Ubuntu (glibc) runtime: multi-arch, and sqlite-jdbc only ships an x86_64 Alpine native lib.
FROM eclipse-temurin:11-jre-jammy
RUN groupadd --system --gid 10001 conduit && useradd --system --uid 10001 --gid conduit --no-create-home conduit \
    && mkdir /data && chown conduit:conduit /data
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER conduit
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/conduit.db
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
