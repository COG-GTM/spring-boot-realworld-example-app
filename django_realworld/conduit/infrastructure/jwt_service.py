"""Port of ``infrastructure.service.DefaultJwtService`` (HS512, subject = user id)."""

import time

import jwt
from django.conf import settings

ALGORITHM = "HS512"


def to_token(user) -> str:
    payload = {"sub": user.id, "exp": int(time.time()) + settings.JWT_SESSION_TIME}
    return jwt.encode(payload, settings.JWT_SECRET.encode(), algorithm=ALGORITHM)


def get_sub_from_token(token: str) -> str | None:
    try:
        claims = jwt.decode(token, settings.JWT_SECRET.encode(), algorithms=[ALGORITHM])
    except jwt.PyJWTError:
        return None
    return claims.get("sub")
