"""Django settings for the RealWorld (Conduit) backend.

Ported from the Spring Boot ``application.properties``. Every value can be
overridden with an environment variable of the same name.
"""

import os
from pathlib import Path

from django.core.exceptions import ImproperlyConfigured

BASE_DIR = Path(__file__).resolve().parent.parent

SECRET_KEY = os.environ.get("DJANGO_SECRET_KEY", "django-insecure-realworld-dev-only")
DEBUG = os.environ.get("DJANGO_DEBUG", "true").lower() == "true"
ALLOWED_HOSTS = os.environ.get("DJANGO_ALLOWED_HOSTS", "*").split(",")

INSTALLED_APPS = [
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "corsheaders",
    "rest_framework",
    "graphene_django",
    "conduit",
]

MIDDLEWARE = [
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.security.SecurityMiddleware",
    "django.middleware.common.CommonMiddleware",
]

ROOT_URLCONF = "realworld.urls"
WSGI_APPLICATION = "realworld.wsgi.application"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {"context_processors": ["django.template.context_processors.request"]},
    }
]

# spring.datasource.url=jdbc:sqlite:dev.db
DATABASES = {
    "default": {
        "ENGINE": os.environ.get("DATABASE_ENGINE", "django.db.backends.sqlite3"),
        "NAME": os.environ.get("DATABASE_NAME", str(BASE_DIR / "dev.db")),
    }
}

# The Java app used BCryptPasswordEncoder; BCrypt stays the default hasher so hashes remain
# interchangeable. Raw Spring hashes ("$2a$10$...") are handled by conduit.infrastructure.hashers.
PASSWORD_HASHERS = [
    "django.contrib.auth.hashers.BCryptPasswordHasher",
    "django.contrib.auth.hashers.PBKDF2PasswordHasher",
]

LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = False
USE_TZ = True

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# REST endpoints have no trailing slash (e.g. /users/login), matching the Spring routes.
APPEND_SLASH = False

# jwt.secret / jwt.sessionTime
JWT_SECRET = os.environ.get(
    "JWT_SECRET",
    "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA",
)
if not DEBUG and "JWT_SECRET" not in os.environ:
    raise ImproperlyConfigured(
        "Set JWT_SECRET when DJANGO_DEBUG is false; the default key is public."
    )
JWT_SESSION_TIME = int(os.environ.get("JWT_SESSION_TIME", "86400"))

# image.default
DEFAULT_IMAGE = os.environ.get(
    "DEFAULT_IMAGE", "https://static.productionready.io/images/smiley-cyrus.jpg"
)

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": ["conduit.api.authentication.JwtTokenAuthentication"],
    "DEFAULT_PERMISSION_CLASSES": ["conduit.api.permissions.RealWorldAccessPolicy"],
    "DEFAULT_RENDERER_CLASSES": ["rest_framework.renderers.JSONRenderer"],
    "DEFAULT_PARSER_CLASSES": ["rest_framework.parsers.JSONParser"],
    "EXCEPTION_HANDLER": "conduit.api.exceptions.realworld_exception_handler",
    "UNAUTHENTICATED_USER": None,
}

GRAPHENE = {"SCHEMA": "conduit.graphql.schema.schema"}

# Mirrors WebSecurityConfig.corsConfigurationSource()
CORS_ALLOW_ALL_ORIGINS = True
CORS_ALLOW_CREDENTIALS = False
CORS_ALLOW_METHODS = ["HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"]
CORS_ALLOW_HEADERS = ["authorization", "cache-control", "content-type"]
