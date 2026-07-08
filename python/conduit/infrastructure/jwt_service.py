from __future__ import annotations

import time
from typing import Optional

import jwt

from conduit.core.service import JwtService
from conduit.core.user import User


class DefaultJwtService(JwtService):
    def __init__(self, secret: str, session_time: int) -> None:
        self.secret = secret
        self.session_time = session_time
        self.algorithm = "HS512"

    def to_token(self, user: User) -> str:
        payload = {
            "sub": user.id,
            "exp": int(time.time()) + self.session_time,
        }
        return jwt.encode(payload, self.secret, algorithm=self.algorithm)

    def get_sub_from_token(self, token: str) -> Optional[str]:
        try:
            claims = jwt.decode(token, self.secret, algorithms=[self.algorithm])
            return claims.get("sub")
        except Exception:
            return None
