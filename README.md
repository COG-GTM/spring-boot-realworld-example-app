# ![RealWorld Example App using Django and Django REST Framework](example-logo.png)

> ### Django + Django REST Framework codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Django + Django REST Framework including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# *NEW* GraphQL Support  

Following some DDD principles. REST or GraphQL is just a kind of adapter. And the domain layer will be consistent all the time. So this repository implement GraphQL and REST at the same time.

The GraphQL schema is [`django_realworld/conduit/graphql/schema.graphqls`](django_realworld/conduit/graphql/schema.graphqls) and the visualization looks like below.

![](graphql-schema.png)

And this implementation is using [graphene-django](https://github.com/graphql-python/graphene-django). The schema is served at `/graphql`, with GraphiQL enabled for exploring it in the browser.

# How it works

The application uses Django + Django REST Framework (DRF).

* Use the idea of Domain Driven Design to separate the business term and infrastructure term.
* Use the Django ORM for persistence: the domain entities are Django models, and the read side builds data transfer objects from ORM queries.
* Use [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern to separate the read model and write model.

And the code is organized as this (all paths are under `django_realworld/`):

1. `realworld/settings.py` holds the settings (JWT secret and session time, default image, database, CORS)
2. `conduit/api` is the web layer implemented with DRF views and serializers, plus `authentication.py`, `permissions.py` and `exceptions.py`
3. `conduit/graphql` is the GraphQL adapter implemented with graphene-django
4. `conduit/models.py` and `conduit/core` are the business model: domain entities and authorization rules (`core/authorization.py`)
5. `conduit/application` is the high-level services: `queries.py` is the read side returning data transfer objects from `data.py`, `commands.py` is the write side including validation, and `cursor_queries.py` handles cursor pagination for GraphQL
6. `conduit/infrastructure` contains the technical details: `jwt_service.py` (HS512 JWT) and `hashers.py` (BCrypt)

# Security

Authentication is handled by a DRF authentication class, `JwtTokenAuthentication` (`conduit/api/authentication.py`). It reads the `Authorization: Token <jwt>` header and validates the HS512-signed JWT. Access rules for public vs. authenticated routes live in the DRF permission class in `conduit/api/permissions.py`.

The secret key is stored in `realworld/settings.py` and can be overridden with the `JWT_SECRET` environment variable.

Passwords are hashed with BCrypt, compatible with existing BCrypt hashes created by the previous Spring implementation.

# Database

It uses a sqlite database (`dev.db`, for easy local test without losing test data after every restart). It can be changed easily in `realworld/settings.py` (or with the `DATABASE_ENGINE` / `DATABASE_NAME` environment variables) for any other database.

The schema is managed with Django migrations:

    python manage.py migrate

# Getting started

You'll need Python 3.11+ installed.

    cd django_realworld
    python3 -m venv .venv
    source .venv/bin/activate
    pip install -r requirements.txt
    python manage.py migrate
    python manage.py runserver 8080

To test that it works, open a browser tab at http://localhost:8080/tags .  
Alternatively, you can run

    curl http://localhost:8080/tags

Django serves on port 8000 by default (`python manage.py runserver`, then http://localhost:8000/tags). The bundled frontend expects the API on port 8080 (`VITE_API_BASE_URL=http://localhost:8080`), so use `runserver 8080` when running it together with the frontend.

# Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.
	
    docker build -t django-realworld-example-app django_realworld
    docker run -p 8080:8080 \
      -v realworld-data:/data -e DATABASE_NAME=/data/dev.db \
      -e JWT_SECRET="$(openssl rand -hex 64)" \
      django-realworld-example-app

The container runs `python manage.py migrate` on start and serves the API on port 8080.

* The SQLite database is stored in the `realworld-data` named volume, so data survives replacing the container. Without the volume, data lives only in that container. `docker volume rm realworld-data` deletes it.
* Set `JWT_SECRET` to your own secret. Otherwise tokens are signed with the public default key from `settings.py`, and anyone can forge them. A new secret invalidates previously issued tokens.
* The image runs Django's development server with `DJANGO_DEBUG=true` by default, which is meant for local demos. Pass `-e DJANGO_DEBUG=false` before exposing it beyond localhost.

# Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

This repository bundles a React frontend in `frontend/` that is already configured for it:

    cd frontend
    npm install
    npm run dev

The frontend is served at http://localhost:3000.

# Run test

The repository contains a lot of test cases to cover both api test and repository test. Run them from `django_realworld/`:

    pytest

# Code format

Use ruff for code format and lint (from `django_realworld/`).

    ruff format .
    ruff check .

# Help

Please fork and PR to improve the project.

## Testing Note

This implementation has been verified to work with the standard RealWorld API specification.
