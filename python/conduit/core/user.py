from __future__ import annotations

from typing import Optional, Protocol

from conduit.core.util import is_empty, new_id


class User:
    def __init__(
        self,
        email: str = "",
        username: str = "",
        password: str = "",
        bio: str = "",
        image: str = "",
        id: Optional[str] = None,
    ) -> None:
        self.id = id if id is not None else new_id()
        self.email = email
        self.username = username
        self.password = password
        self.bio = bio
        self.image = image

    def update(
        self, email: str, username: str, password: str, bio: str, image: str
    ) -> None:
        if not is_empty(email):
            self.email = email
        if not is_empty(username):
            self.username = username
        if not is_empty(password):
            self.password = password
        if not is_empty(bio):
            self.bio = bio
        if not is_empty(image):
            self.image = image

    def __eq__(self, other: object) -> bool:
        return isinstance(other, User) and other.id == self.id

    def __hash__(self) -> int:
        return hash(self.id)


class FollowRelation:
    def __init__(self, user_id: str = "", target_id: str = "") -> None:
        self.user_id = user_id
        self.target_id = target_id

    def __eq__(self, other: object) -> bool:
        return (
            isinstance(other, FollowRelation)
            and other.user_id == self.user_id
            and other.target_id == self.target_id
        )

    def __hash__(self) -> int:
        return hash((self.user_id, self.target_id))


class UserRepository(Protocol):
    def save(self, user: User) -> None: ...

    def find_by_id(self, id: str) -> Optional[User]: ...

    def find_by_username(self, username: str) -> Optional[User]: ...

    def find_by_email(self, email: str) -> Optional[User]: ...

    def save_relation(self, follow_relation: FollowRelation) -> None: ...

    def find_relation(
        self, user_id: str, target_id: str
    ) -> Optional[FollowRelation]: ...

    def remove_relation(self, follow_relation: FollowRelation) -> None: ...
