from __future__ import annotations

from conduit.application.params import (
    NewArticleParam,
    RegisterParam,
    UpdateArticleParam,
    UpdateUserParam,
)
from conduit.core.article import Article, ArticleRepository
from conduit.core.user import User, UserRepository


class UserService:
    def __init__(
        self,
        user_repository: UserRepository,
        default_image: str,
        password_encoder,
    ) -> None:
        self.user_repository = user_repository
        self.default_image = default_image
        self.password_encoder = password_encoder

    def create_user(self, param: RegisterParam) -> User:
        user = User(
            email=param.email,
            username=param.username,
            password=self.password_encoder.encode(param.password),
            bio="",
            image=self.default_image,
        )
        self.user_repository.save(user)
        return user

    def update_user(self, target_user: User, param: UpdateUserParam) -> None:
        target_user.update(
            param.email,
            param.username,
            param.password,
            param.bio,
            param.image,
        )
        self.user_repository.save(target_user)


class ArticleCommandService:
    def __init__(self, article_repository: ArticleRepository) -> None:
        self.article_repository = article_repository

    def create_article(self, param: NewArticleParam, creator: User) -> Article:
        article = Article(
            title=param.title,
            description=param.description,
            body=param.body,
            tag_list=param.tag_list or [],
            user_id=creator.id,
        )
        self.article_repository.save(article)
        return article

    def update_article(
        self, article: Article, param: UpdateArticleParam
    ) -> Article:
        article.update(param.title, param.description, param.body)
        self.article_repository.save(article)
        return article
