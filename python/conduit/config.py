from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass
class Settings:
    datasource_url: str = "sqlite:///dev.db"
    jwt_secret: str = (
        "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-"
        "TiuDapkLiUCogO3JOK7kwZisrHp6wA"
    )
    jwt_session_time: int = 86400
    default_image: str = "https://static.productionready.io/images/smiley-cyrus.jpg"

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            datasource_url=os.environ.get("DATASOURCE_URL", cls.datasource_url),
            jwt_secret=os.environ.get("JWT_SECRET", cls.jwt_secret),
            jwt_session_time=int(
                os.environ.get("JWT_SESSION_TIME", cls.jwt_session_time)
            ),
            default_image=os.environ.get("IMAGE_DEFAULT", cls.default_image),
        )
