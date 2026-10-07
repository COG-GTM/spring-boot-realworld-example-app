"""REST routes (no trailing slashes), one view module per Spring controller group."""

from django.urls import path

from conduit.api import articles, comments, profiles, tags, users

urlpatterns = [
    # UsersApi / CurrentUserApi
    path("users", users.RegisterView.as_view()),
    path("users/login", users.LoginView.as_view()),
    path("user", users.CurrentUserView.as_view()),
    # ProfileApi
    path("profiles/<str:username>", profiles.ProfileView.as_view()),
    path("profiles/<str:username>/follow", profiles.FollowView.as_view()),
    # ArticlesApi / ArticleApi / ArticleFavoriteApi
    path("articles", articles.ArticlesView.as_view()),
    path("articles/feed", articles.FeedView.as_view()),
    path("articles/<str:slug>", articles.ArticleView.as_view()),
    path("articles/<str:slug>/favorite", articles.FavoriteView.as_view()),
    # CommentsApi
    path("articles/<str:slug>/comments", comments.CommentsView.as_view()),
    path("articles/<str:slug>/comments/<str:comment_id>", comments.CommentView.as_view()),
    # TagsApi
    path("tags", tags.TagsView.as_view()),
]
