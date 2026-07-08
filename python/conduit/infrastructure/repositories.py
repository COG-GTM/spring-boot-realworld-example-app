from __future__ import annotations

from typing import Optional

from sqlalchemy import text
from sqlalchemy.engine import Connection, Engine

from conduit.core.article import Article, ArticleRepository, Tag
from conduit.core.comment import Comment, CommentRepository
from conduit.core.favorite import ArticleFavorite, ArticleFavoriteRepository
from conduit.core.user import FollowRelation, User, UserRepository
from conduit.infrastructure.db import from_db, to_db


class SqlUserRepository(UserRepository):
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def save(self, user: User) -> None:
        with self.engine.begin() as conn:
            if self._find_by_id(conn, user.id) is None:
                conn.execute(
                    text(
                        "insert into users (id, username, email, password, bio, image) "
                        "values (:id, :username, :email, :password, :bio, :image)"
                    ),
                    {
                        "id": user.id,
                        "username": user.username,
                        "email": user.email,
                        "password": user.password,
                        "bio": user.bio,
                        "image": user.image,
                    },
                )
            else:
                sets = []
                params = {"id": user.id}
                if user.username != "":
                    sets.append("username = :username")
                    params["username"] = user.username
                if user.email != "":
                    sets.append("email = :email")
                    params["email"] = user.email
                if user.password != "":
                    sets.append("password = :password")
                    params["password"] = user.password
                if user.bio != "":
                    sets.append("bio = :bio")
                    params["bio"] = user.bio
                if user.image != "":
                    sets.append("image = :image")
                    params["image"] = user.image
                if sets:
                    conn.execute(
                        text(f"update users set {', '.join(sets)} where id = :id"),
                        params,
                    )

    @staticmethod
    def _row_to_user(row) -> User:
        return User(
            id=row.id,
            username=row.username,
            email=row.email,
            password=row.password,
            bio=row.bio,
            image=row.image,
        )

    def _find_by_id(self, conn: Connection, id: str) -> Optional[User]:
        row = conn.execute(
            text(
                "select id, username, email, password, bio, image "
                "from users where id = :id"
            ),
            {"id": id},
        ).first()
        return self._row_to_user(row) if row else None

    def find_by_id(self, id: str) -> Optional[User]:
        with self.engine.connect() as conn:
            return self._find_by_id(conn, id)

    def find_by_username(self, username: str) -> Optional[User]:
        with self.engine.connect() as conn:
            row = conn.execute(
                text("select * from users where username = :username"),
                {"username": username},
            ).first()
            return self._row_to_user(row) if row else None

    def find_by_email(self, email: str) -> Optional[User]:
        with self.engine.connect() as conn:
            row = conn.execute(
                text(
                    "select id, username, email, password, bio, image "
                    "from users where email = :email"
                ),
                {"email": email},
            ).first()
            return self._row_to_user(row) if row else None

    def save_relation(self, follow_relation: FollowRelation) -> None:
        if self.find_relation(follow_relation.user_id, follow_relation.target_id):
            return
        with self.engine.begin() as conn:
            conn.execute(
                text(
                    "insert into follows(user_id, follow_id) "
                    "values (:user_id, :follow_id)"
                ),
                {
                    "user_id": follow_relation.user_id,
                    "follow_id": follow_relation.target_id,
                },
            )

    def find_relation(
        self, user_id: str, target_id: str
    ) -> Optional[FollowRelation]:
        with self.engine.connect() as conn:
            row = conn.execute(
                text(
                    "select user_id, follow_id from follows "
                    "where user_id = :user_id and follow_id = :target_id"
                ),
                {"user_id": user_id, "target_id": target_id},
            ).first()
            if row is None:
                return None
            return FollowRelation(row.user_id, row.follow_id)

    def remove_relation(self, follow_relation: FollowRelation) -> None:
        with self.engine.begin() as conn:
            conn.execute(
                text(
                    "delete from follows where user_id = :user_id "
                    "and follow_id = :follow_id"
                ),
                {
                    "user_id": follow_relation.user_id,
                    "follow_id": follow_relation.target_id,
                },
            )


class SqlArticleRepository(ArticleRepository):
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def _find_article_id(self, conn: Connection, id: str) -> Optional[str]:
        return conn.execute(
            text("select id from articles where id = :id"), {"id": id}
        ).scalar()

    def _find_tag(self, conn: Connection, name: str) -> Optional[Tag]:
        row = conn.execute(
            text("select id, name from tags where name = :name"), {"name": name}
        ).first()
        return Tag(id=row.id, name=row.name) if row else None

    def save(self, article: Article) -> None:
        with self.engine.begin() as conn:
            if self._find_article_id(conn, article.id) is None:
                self._create_new(conn, article)
            else:
                self._update(conn, article)

    def _create_new(self, conn: Connection, article: Article) -> None:
        for tag in article.tags:
            target_tag = self._find_tag(conn, tag.name)
            if target_tag is None:
                conn.execute(
                    text("insert into tags (id, name) values (:id, :name)"),
                    {"id": tag.id, "name": tag.name},
                )
                target_tag = tag
            conn.execute(
                text(
                    "insert into article_tags (article_id, tag_id) "
                    "values (:article_id, :tag_id)"
                ),
                {"article_id": article.id, "tag_id": target_tag.id},
            )
        conn.execute(
            text(
                "insert into articles(id, slug, title, description, body, user_id, "
                "created_at, updated_at) values (:id, :slug, :title, :description, "
                ":body, :user_id, :created_at, :updated_at)"
            ),
            {
                "id": article.id,
                "slug": article.slug,
                "title": article.title,
                "description": article.description,
                "body": article.body,
                "user_id": article.user_id,
                "created_at": to_db(article.created_at),
                "updated_at": to_db(article.updated_at),
            },
        )

    def _update(self, conn: Connection, article: Article) -> None:
        sets = []
        params = {"id": article.id}
        if article.title != "":
            sets.append("title = :title")
            params["title"] = article.title
            sets.append("slug = :slug")
            params["slug"] = article.slug
        if article.description != "":
            sets.append("description = :description")
            params["description"] = article.description
        if article.body != "":
            sets.append("body = :body")
            params["body"] = article.body
        if sets:
            conn.execute(
                text(f"update articles set {', '.join(sets)} where id = :id"),
                params,
            )

    def _load(self, conn: Connection, where: str, params: dict) -> Optional[Article]:
        rows = conn.execute(
            text(
                "select A.id articleId, A.user_id articleUserId, A.slug articleSlug, "
                "A.title articleTitle, A.description articleDescription, "
                "A.body articleBody, A.created_at articleCreatedAt, "
                "A.updated_at articleUpdatedAt, T.id tagId, T.name tagName "
                "from articles A "
                "left join article_tags AT on A.id = AT.article_id "
                "left join tags T on T.id = AT.tag_id "
                f"where {where}"
            ),
            params,
        ).fetchall()
        if not rows:
            return None
        first = rows[0]
        article = Article(
            id=first.articleId,
            title=first.articleTitle,
            description=first.articleDescription,
            body=first.articleBody,
            tag_list=[],
            user_id=first.articleUserId,
            created_at=from_db(first.articleCreatedAt),
        )
        article.slug = first.articleSlug
        article.updated_at = from_db(first.articleUpdatedAt)
        tags = []
        for row in rows:
            if row.tagId is not None:
                tags.append(Tag(id=row.tagId, name=row.tagName))
        article.tags = tags
        return article

    def find_by_id(self, id: str) -> Optional[Article]:
        with self.engine.connect() as conn:
            return self._load(conn, "A.id = :id", {"id": id})

    def find_by_slug(self, slug: str) -> Optional[Article]:
        with self.engine.connect() as conn:
            return self._load(conn, "A.slug = :slug", {"slug": slug})

    def remove(self, article: Article) -> None:
        with self.engine.begin() as conn:
            conn.execute(
                text("delete from articles where id = :id"), {"id": article.id}
            )


class SqlCommentRepository(CommentRepository):
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def save(self, comment: Comment) -> None:
        with self.engine.begin() as conn:
            conn.execute(
                text(
                    "insert into comments(id, body, user_id, article_id, created_at, "
                    "updated_at) values (:id, :body, :user_id, :article_id, "
                    ":created_at, :updated_at)"
                ),
                {
                    "id": comment.id,
                    "body": comment.body,
                    "user_id": comment.user_id,
                    "article_id": comment.article_id,
                    "created_at": to_db(comment.created_at),
                    "updated_at": to_db(comment.created_at),
                },
            )

    def find_by_id(self, article_id: str, id: str) -> Optional[Comment]:
        with self.engine.connect() as conn:
            row = conn.execute(
                text(
                    "select id, body, user_id, article_id, created_at from comments "
                    "where id = :id and article_id = :article_id"
                ),
                {"id": id, "article_id": article_id},
            ).first()
            if row is None:
                return None
            return Comment(
                id=row.id,
                body=row.body,
                user_id=row.user_id,
                article_id=row.article_id,
                created_at=from_db(row.created_at),
            )

    def remove(self, comment: Comment) -> None:
        with self.engine.begin() as conn:
            conn.execute(
                text("delete from comments where id = :id"), {"id": comment.id}
            )


class SqlArticleFavoriteRepository(ArticleFavoriteRepository):
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def save(self, article_favorite: ArticleFavorite) -> None:
        if self.find(article_favorite.article_id, article_favorite.user_id):
            return
        with self.engine.begin() as conn:
            conn.execute(
                text(
                    "insert into article_favorites (article_id, user_id) "
                    "values (:article_id, :user_id)"
                ),
                {
                    "article_id": article_favorite.article_id,
                    "user_id": article_favorite.user_id,
                },
            )

    def find(self, article_id: str, user_id: str) -> Optional[ArticleFavorite]:
        with self.engine.connect() as conn:
            row = conn.execute(
                text(
                    "select article_id, user_id from article_favorites "
                    "where article_id = :article_id and user_id = :user_id"
                ),
                {"article_id": article_id, "user_id": user_id},
            ).first()
            if row is None:
                return None
            return ArticleFavorite(row.article_id, row.user_id)

    def remove(self, favorite: ArticleFavorite) -> None:
        with self.engine.begin() as conn:
            conn.execute(
                text(
                    "delete from article_favorites where article_id = :article_id "
                    "and user_id = :user_id"
                ),
                {"article_id": favorite.article_id, "user_id": favorite.user_id},
            )
