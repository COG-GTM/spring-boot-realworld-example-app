from __future__ import annotations

from typing import Dict, List, Optional, Set

from sqlalchemy import text
from sqlalchemy.engine import Connection, Engine

from conduit.application.data import (
    ArticleData,
    ArticleFavoriteCount,
    CommentData,
    ProfileData,
    UserData,
)
from conduit.application.page import CursorPageParameter, Direction, Page
from conduit.core.user import User
from conduit.infrastructure.db import from_db, to_db

_PROFILE_COLUMNS = (
    "U.id userId, U.username userUsername, U.bio userBio, U.image userImage"
)

_SELECT_ARTICLE_DATA = f"""
    select
    A.id articleId, A.slug articleSlug, A.title articleTitle,
    A.description articleDescription, A.body articleBody,
    A.created_at articleCreatedAt, A.updated_at articleUpdatedAt,
    T.name tagName, {_PROFILE_COLUMNS}
    from articles A
    left join article_tags AT on A.id = AT.article_id
    left join tags T on T.id = AT.tag_id
    left join users U on U.id = A.user_id
"""


def _in_clause(name: str, ids: List[str], params: dict) -> str:
    keys = []
    for i, value in enumerate(ids):
        key = f"{name}{i}"
        params[key] = value
        keys.append(f":{key}")
    return "(" + ", ".join(keys) + ")"


class UserReadService:
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    @staticmethod
    def _row_to_user_data(row) -> UserData:
        return UserData(
            id=row.id,
            email=row.email,
            username=row.username,
            bio=row.bio,
            image=row.image,
        )

    def find_by_username(self, username: str) -> Optional[UserData]:
        with self.engine.connect() as conn:
            row = conn.execute(
                text("select * from users where username = :username"),
                {"username": username},
            ).first()
            return self._row_to_user_data(row) if row else None

    def find_by_id(self, id: str) -> Optional[UserData]:
        with self.engine.connect() as conn:
            row = conn.execute(
                text("select * from users where id = :id"), {"id": id}
            ).first()
            return self._row_to_user_data(row) if row else None


class UserRelationshipQueryService:
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def is_user_following(self, user_id: str, another_user_id: str) -> bool:
        with self.engine.connect() as conn:
            count = conn.execute(
                text(
                    "select count(1) from follows "
                    "where user_id = :user_id and follow_id = :another"
                ),
                {"user_id": user_id, "another": another_user_id},
            ).scalar()
            return bool(count)

    def following_authors(self, user_id: str, ids: List[str]) -> Set[str]:
        if not ids:
            return set()
        with self.engine.connect() as conn:
            params: dict = {"user_id": user_id}
            in_clause = _in_clause("id", ids, params)
            rows = conn.execute(
                text(
                    f"select F.follow_id from follows F where F.follow_id in {in_clause} "
                    "and F.user_id = :user_id"
                ),
                params,
            ).fetchall()
            return {row.follow_id for row in rows}

    def followed_users(self, user_id: str) -> List[str]:
        with self.engine.connect() as conn:
            rows = conn.execute(
                text("select F.follow_id from follows F where F.user_id = :user_id"),
                {"user_id": user_id},
            ).fetchall()
            return [row.follow_id for row in rows]


class TagReadService:
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def all(self) -> List[str]:
        with self.engine.connect() as conn:
            rows = conn.execute(text("select name from tags")).fetchall()
            return [row.name for row in rows]


class ArticleFavoritesReadService:
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def is_user_favorite(self, user_id: str, article_id: str) -> bool:
        with self.engine.connect() as conn:
            count = conn.execute(
                text(
                    "select count(1) from article_favorites "
                    "where user_id = :user_id and article_id = :article_id"
                ),
                {"user_id": user_id, "article_id": article_id},
            ).scalar()
            return bool(count)

    def article_favorite_count(self, article_id: str) -> int:
        with self.engine.connect() as conn:
            return int(
                conn.execute(
                    text(
                        "select count(1) from article_favorites "
                        "where article_id = :article_id"
                    ),
                    {"article_id": article_id},
                ).scalar()
                or 0
            )

    def articles_favorite_count(self, ids: List[str]) -> List[ArticleFavoriteCount]:
        if not ids:
            return []
        with self.engine.connect() as conn:
            params: dict = {}
            in_clause = _in_clause("id", ids, params)
            rows = conn.execute(
                text(
                    "select A.id, count(AF.user_id) as favoriteCount from articles A "
                    "left join article_favorites AF on A.id = AF.article_id "
                    f"where id in {in_clause} group by A.id"
                ),
                params,
            ).fetchall()
            return [ArticleFavoriteCount(row.id, int(row.favoriteCount)) for row in rows]

    def user_favorites(self, ids: List[str], current_user: User) -> Set[str]:
        if not ids:
            return set()
        with self.engine.connect() as conn:
            params: dict = {"user_id": current_user.id}
            in_clause = _in_clause("id", ids, params)
            rows = conn.execute(
                text(
                    "select A.id from articles A "
                    "left join article_favorites AF on A.id = AF.article_id "
                    f"where id in {in_clause} and AF.user_id = :user_id"
                ),
                params,
            ).fetchall()
            return {row.id for row in rows}


class ArticleReadService:
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def _map_article_data(self, rows) -> List[ArticleData]:
        by_id: Dict[str, ArticleData] = {}
        order: List[str] = []
        for row in rows:
            article_id = row.articleId
            if article_id not in by_id:
                order.append(article_id)
                by_id[article_id] = ArticleData(
                    id=article_id,
                    slug=row.articleSlug,
                    title=row.articleTitle,
                    description=row.articleDescription,
                    body=row.articleBody,
                    favorited=False,
                    favorites_count=0,
                    created_at=from_db(row.articleCreatedAt),
                    updated_at=from_db(row.articleUpdatedAt),
                    tag_list=[],
                    profile_data=ProfileData(
                        id=row.userId,
                        username=row.userUsername,
                        bio=row.userBio,
                        image=row.userImage,
                        following=False,
                    ),
                )
            if row.tagName is not None:
                by_id[article_id].tag_list.append(row.tagName)
        return [by_id[i] for i in order]

    def find_by_id(self, id: str) -> Optional[ArticleData]:
        with self.engine.connect() as conn:
            rows = conn.execute(
                text(_SELECT_ARTICLE_DATA + " where A.id = :id"), {"id": id}
            ).fetchall()
            data = self._map_article_data(rows)
            return data[0] if data else None

    def find_by_slug(self, slug: str) -> Optional[ArticleData]:
        with self.engine.connect() as conn:
            rows = conn.execute(
                text(_SELECT_ARTICLE_DATA + " where A.slug = :slug"), {"slug": slug}
            ).fetchall()
            data = self._map_article_data(rows)
            return data[0] if data else None

    @staticmethod
    def _filters(tag, author, favorited_by, params: dict) -> str:
        clauses = []
        if tag is not None:
            clauses.append("T.name = :tag")
            params["tag"] = tag
        if author is not None:
            clauses.append("AU.username = :author")
            params["author"] = author
        if favorited_by is not None:
            clauses.append("AFU.username = :favoritedBy")
            params["favoritedBy"] = favorited_by
        return (" where " + " AND ".join(clauses)) if clauses else ""

    _SELECT_ARTICLE_IDS = """
        select DISTINCT(A.id) articleId, A.created_at
        from articles A
        left join article_tags AT on A.id = AT.article_id
        left join tags T on T.id = AT.tag_id
        left join article_favorites AF on AF.article_id = A.id
        left join users AU on AU.id = A.user_id
        left join users AFU on AFU.id = AF.user_id
    """

    def query_articles(
        self, tag, author, favorited_by, page: Page
    ) -> List[str]:
        with self.engine.connect() as conn:
            params: dict = {"limit": page.limit, "offset": page.offset}
            where = self._filters(tag, author, favorited_by, params)
            rows = conn.execute(
                text(
                    self._SELECT_ARTICLE_IDS
                    + where
                    + " order by A.created_at desc limit :limit offset :offset"
                ),
                params,
            ).fetchall()
            return [row.articleId for row in rows]

    def count_article(self, tag, author, favorited_by) -> int:
        with self.engine.connect() as conn:
            params: dict = {}
            where = self._filters(tag, author, favorited_by, params)
            return int(
                conn.execute(
                    text(
                        "select count(DISTINCT A.id) from articles A "
                        "left join article_tags AT on A.id = AT.article_id "
                        "left join tags T on T.id = AT.tag_id "
                        "left join article_favorites AF on AF.article_id = A.id "
                        "left join users AU on AU.id = A.user_id "
                        "left join users AFU on AFU.id = AF.user_id" + where
                    ),
                    params,
                ).scalar()
                or 0
            )

    def find_articles(self, article_ids: List[str]) -> List[ArticleData]:
        if not article_ids:
            return []
        with self.engine.connect() as conn:
            params: dict = {}
            in_clause = _in_clause("id", article_ids, params)
            rows = conn.execute(
                text(
                    _SELECT_ARTICLE_DATA
                    + f" where A.id in {in_clause} order by A.created_at desc"
                ),
                params,
            ).fetchall()
            return self._map_article_data(rows)

    def find_articles_of_authors(
        self, authors: List[str], page: Page
    ) -> List[ArticleData]:
        if not authors:
            return []
        with self.engine.connect() as conn:
            params: dict = {"limit": page.limit, "offset": page.offset}
            in_clause = _in_clause("author", authors, params)
            rows = conn.execute(
                text(
                    _SELECT_ARTICLE_DATA
                    + f" where A.user_id in {in_clause} limit :limit offset :offset"
                ),
                params,
            ).fetchall()
            return self._map_article_data(rows)

    def count_feed_size(self, authors: List[str]) -> int:
        if not authors:
            return 0
        with self.engine.connect() as conn:
            params: dict = {}
            in_clause = _in_clause("author", authors, params)
            return int(
                conn.execute(
                    text(
                        f"select count(1) from articles A where A.user_id in {in_clause}"
                    ),
                    params,
                ).scalar()
                or 0
            )

    def find_articles_with_cursor(
        self, tag, author, favorited_by, page: CursorPageParameter
    ) -> List[str]:
        with self.engine.connect() as conn:
            params: dict = {"limit": page.query_limit}
            clauses = []
            if tag is not None:
                clauses.append("T.name = :tag")
                params["tag"] = tag
            if author is not None:
                clauses.append("AU.username = :author")
                params["author"] = author
            if favorited_by is not None:
                clauses.append("AFU.username = :favoritedBy")
                params["favoritedBy"] = favorited_by
            if page.cursor is not None and page.direction == Direction.NEXT:
                clauses.append("A.created_at < :cursor")
                params["cursor"] = to_db(page.cursor)
            if page.cursor is not None and page.direction == Direction.PREV:
                clauses.append("A.created_at > :cursor")
                params["cursor"] = to_db(page.cursor)
            where = (" where " + " AND ".join(clauses)) if clauses else ""
            order = (
                " order by A.created_at desc"
                if page.direction == Direction.NEXT
                else " order by A.created_at asc"
            )
            rows = conn.execute(
                text(self._SELECT_ARTICLE_IDS + where + order + " limit :limit"),
                params,
            ).fetchall()
            return [row.articleId for row in rows]

    def find_articles_of_authors_with_cursor(
        self, authors: List[str], page: CursorPageParameter
    ) -> List[ArticleData]:
        if not authors:
            return []
        with self.engine.connect() as conn:
            params: dict = {"limit": page.query_limit}
            in_clause = _in_clause("author", authors, params)
            clauses = [f"A.user_id in {in_clause}"]
            if page.cursor is not None and page.direction == Direction.NEXT:
                clauses.append("A.created_at < :cursor")
                params["cursor"] = to_db(page.cursor)
            if page.cursor is not None and page.direction == Direction.PREV:
                clauses.append("A.created_at > :cursor")
                params["cursor"] = to_db(page.cursor)
            order = (
                " order by A.created_at desc"
                if page.direction == Direction.NEXT
                else " order by A.created_at asc"
            )
            rows = conn.execute(
                text(
                    _SELECT_ARTICLE_DATA
                    + " where "
                    + " AND ".join(clauses)
                    + order
                    + " limit :limit"
                ),
                params,
            ).fetchall()
            return self._map_article_data(rows)


class CommentReadService:
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    _SELECT_COMMENT_DATA = f"""
        select C.id commentId, C.body commentBody, C.created_at commentCreatedAt,
        C.article_id commentArticleId, {_PROFILE_COLUMNS}
        from comments C
        left join users U on C.user_id = U.id
    """

    def _map(self, rows) -> List[CommentData]:
        result = []
        for row in rows:
            created = from_db(row.commentCreatedAt)
            result.append(
                CommentData(
                    id=row.commentId,
                    body=row.commentBody,
                    article_id=row.commentArticleId,
                    created_at=created,
                    updated_at=created,
                    profile_data=ProfileData(
                        id=row.userId,
                        username=row.userUsername,
                        bio=row.userBio,
                        image=row.userImage,
                        following=False,
                    ),
                )
            )
        return result

    def find_by_id(self, id: str) -> Optional[CommentData]:
        with self.engine.connect() as conn:
            rows = conn.execute(
                text(self._SELECT_COMMENT_DATA + " where C.id = :id"), {"id": id}
            ).fetchall()
            data = self._map(rows)
            return data[0] if data else None

    def find_by_article_id(self, article_id: str) -> List[CommentData]:
        with self.engine.connect() as conn:
            rows = conn.execute(
                text(self._SELECT_COMMENT_DATA + " where C.article_id = :article_id"),
                {"article_id": article_id},
            ).fetchall()
            return self._map(rows)

    def find_by_article_id_with_cursor(
        self, article_id: str, page: CursorPageParameter
    ) -> List[CommentData]:
        with self.engine.connect() as conn:
            params: dict = {"article_id": article_id}
            clauses = ["C.article_id = :article_id"]
            if page.cursor is not None and page.direction == Direction.NEXT:
                clauses.append("C.created_at < :cursor")
                params["cursor"] = to_db(page.cursor)
            if page.cursor is not None and page.direction == Direction.PREV:
                clauses.append("C.created_at > :cursor")
                params["cursor"] = to_db(page.cursor)
            order = (
                " order by C.created_at desc"
                if page.direction == Direction.NEXT
                else " order by C.created_at asc"
            )
            rows = conn.execute(
                text(
                    self._SELECT_COMMENT_DATA
                    + " where "
                    + " AND ".join(clauses)
                    + order
                ),
                params,
            ).fetchall()
            return self._map(rows)
