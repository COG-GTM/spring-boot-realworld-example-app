"""JWT helpers mirroring DefaultJwtService (HS512, sub=user id, 24h expiry)."""

from datetime import datetime, timedelta, timezone

import jwt
from django.conf import settings


def generate_token(user):
    payload = {
        "sub": str(user.id),
        "exp": datetime.now(tz=timezone.utc)
        + timedelta(seconds=settings.JWT_EXPIRATION_SECONDS),
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)


def decode_token(token):
    """Return the subject (user id) string, or None on any failure.

    Mirrors DefaultJwtService.getSubFromToken which swallows all exceptions.
    """
    try:
        payload = jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM])
        return payload.get("sub")
    except Exception:
        return None
