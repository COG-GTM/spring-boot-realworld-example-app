"""Django settings for the Conduit (RealWorld) backend.

Migrated from the Spring Boot + MyBatis implementation. The REST API contract
is preserved exactly so the existing React frontend keeps working unchanged:
all routes live at the root (no ``/api`` prefix), auth uses the
``Authorization: Token <jwt>`` scheme, and responses are root-wrapped.
"""

from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

# Mirrors jwt.secret in src/main/resources/application.properties. Kept inline
# for parity with the Java app (dev reference project, SQLite-backed).
SECRET_KEY = "django-insecure-conduit-realworld-migration-key"

DEBUG = True

ALLOWED_HOSTS = ["*"]

INSTALLED_APPS = [
    "django.contrib.contenttypes",
    "django.contrib.auth",
    "django.contrib.staticfiles",
    "rest_framework",
    "corsheaders",
    "authentication",
    "profiles",
]

MIDDLEWARE = [
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
]

ROOT_URLCONF = "conduit.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {"context_processors": []},
    },
]

WSGI_APPLICATION = "conduit.wsgi.application"
ASGI_APPLICATION = "conduit.asgi.application"

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.sqlite3",
        "NAME": BASE_DIR / "db.sqlite3",
    }
}

AUTH_USER_MODEL = "authentication.User"

# bcrypt first for parity with Spring Security's BCryptPasswordEncoder.
PASSWORD_HASHERS = [
    "django.contrib.auth.hashers.BCryptSHA256PasswordHasher",
    "django.contrib.auth.hashers.BCryptPasswordHasher",
    "django.contrib.auth.hashers.PBKDF2PasswordHasher",
]

LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# --- Conduit / JWT contract (mirrors application.properties + DefaultJwtService) ---
JWT_SECRET = (
    "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA"
)
JWT_ALGORITHM = "HS512"
JWT_EXPIRATION_SECONDS = 86400
DEFAULT_USER_IMAGE = "https://static.productionready.io/images/smiley-cyrus.jpg"

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "authentication.backends.JWTAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
    "EXCEPTION_HANDLER": "conduit.exceptions.custom_exception_handler",
    "UNAUTHENTICATED_USER": None,
}

# Mirrors WebSecurityConfig.corsConfigurationSource().
CORS_ALLOW_ALL_ORIGINS = True
CORS_ALLOW_CREDENTIALS = False
CORS_ALLOW_METHODS = ["HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"]
CORS_ALLOW_HEADERS = ["authorization", "cache-control", "content-type"]
