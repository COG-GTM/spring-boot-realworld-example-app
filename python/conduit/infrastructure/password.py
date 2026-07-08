from __future__ import annotations

from passlib.context import CryptContext


class BCryptPasswordEncoder:
    def __init__(self) -> None:
        self._context = CryptContext(schemes=["bcrypt"], deprecated="auto")

    def encode(self, raw_password: str) -> str:
        return self._context.hash(raw_password)

    def matches(self, raw_password: str, encoded_password: str) -> bool:
        try:
            return self._context.verify(raw_password, encoded_password)
        except Exception:
            return False
