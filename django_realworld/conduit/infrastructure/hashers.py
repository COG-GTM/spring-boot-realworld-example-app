"""Password hashing compatible with Spring's ``BCryptPasswordEncoder``.

Spring stores bare BCrypt hashes (``$2a$10$...``); Django prefixes the algorithm
(``bcrypt$$2b$12$...``). Both forms are accepted.
"""

from django.contrib.auth.hashers import check_password as django_check_password
from django.contrib.auth.hashers import make_password


def hash_password(raw: str) -> str:
    return make_password(raw)


def verify_password(raw: str, encoded: str | None) -> bool:
    if not encoded:
        return False
    if encoded.startswith("$2"):
        encoded = "bcrypt$" + encoded
    return django_check_password(raw, encoded)
