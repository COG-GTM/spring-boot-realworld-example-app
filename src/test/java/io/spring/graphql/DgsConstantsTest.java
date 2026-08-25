package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DgsConstantsTest {
  @Test
  public void should_expose_query_constants() {
    assertThat(DgsConstants.QUERY_TYPE).isEqualTo("Query");
    assertThat(new DgsConstants.QUERY()).isNotNull();
    assertThat(DgsConstants.QUERY.TYPE_NAME).isEqualTo("Query");
    assertThat(DgsConstants.QUERY.Article).isEqualTo("article");
    assertThat(DgsConstants.QUERY.Articles).isEqualTo("articles");
    assertThat(DgsConstants.QUERY.Me).isEqualTo("me");
    assertThat(DgsConstants.QUERY.Feed).isEqualTo("feed");
    assertThat(DgsConstants.QUERY.Profile).isEqualTo("profile");
    assertThat(DgsConstants.QUERY.Tags).isEqualTo("tags");
  }

  @Test
  public void should_expose_mutation_constants() {
    assertThat(new DgsConstants.MUTATION()).isNotNull();
    assertThat(DgsConstants.MUTATION.TYPE_NAME).isEqualTo("Mutation");
    assertThat(DgsConstants.MUTATION.CreateUser).isEqualTo("createUser");
    assertThat(DgsConstants.MUTATION.Login).isEqualTo("login");
    assertThat(DgsConstants.MUTATION.UpdateUser).isEqualTo("updateUser");
    assertThat(DgsConstants.MUTATION.FollowUser).isEqualTo("followUser");
    assertThat(DgsConstants.MUTATION.UnfollowUser).isEqualTo("unfollowUser");
    assertThat(DgsConstants.MUTATION.CreateArticle).isEqualTo("createArticle");
    assertThat(DgsConstants.MUTATION.UpdateArticle).isEqualTo("updateArticle");
    assertThat(DgsConstants.MUTATION.FavoriteArticle).isEqualTo("favoriteArticle");
    assertThat(DgsConstants.MUTATION.UnfavoriteArticle).isEqualTo("unfavoriteArticle");
    assertThat(DgsConstants.MUTATION.DeleteArticle).isEqualTo("deleteArticle");
    assertThat(DgsConstants.MUTATION.AddComment).isEqualTo("addComment");
    assertThat(DgsConstants.MUTATION.DeleteComment).isEqualTo("deleteComment");
  }

  @Test
  public void should_expose_article_constants() {
    assertThat(new DgsConstants.ARTICLE()).isNotNull();
    assertThat(DgsConstants.ARTICLE.TYPE_NAME).isEqualTo("Article");
    assertThat(DgsConstants.ARTICLE.Author).isEqualTo("author");
    assertThat(DgsConstants.ARTICLE.Body).isEqualTo("body");
    assertThat(DgsConstants.ARTICLE.Comments).isEqualTo("comments");
    assertThat(DgsConstants.ARTICLE.CreatedAt).isEqualTo("createdAt");
    assertThat(DgsConstants.ARTICLE.Description).isEqualTo("description");
    assertThat(DgsConstants.ARTICLE.Favorited).isEqualTo("favorited");
    assertThat(DgsConstants.ARTICLE.FavoritesCount).isEqualTo("favoritesCount");
    assertThat(DgsConstants.ARTICLE.Slug).isEqualTo("slug");
    assertThat(DgsConstants.ARTICLE.TagList).isEqualTo("tagList");
    assertThat(DgsConstants.ARTICLE.Title).isEqualTo("title");
    assertThat(DgsConstants.ARTICLE.UpdatedAt).isEqualTo("updatedAt");
  }

  @Test
  public void should_expose_article_connection_constants() {
    assertThat(new DgsConstants.ARTICLEEDGE()).isNotNull();
    assertThat(DgsConstants.ARTICLEEDGE.TYPE_NAME).isEqualTo("ArticleEdge");
    assertThat(DgsConstants.ARTICLEEDGE.Cursor).isEqualTo("cursor");
    assertThat(DgsConstants.ARTICLEEDGE.Node).isEqualTo("node");

    assertThat(new DgsConstants.ARTICLESCONNECTION()).isNotNull();
    assertThat(DgsConstants.ARTICLESCONNECTION.TYPE_NAME).isEqualTo("ArticlesConnection");
    assertThat(DgsConstants.ARTICLESCONNECTION.Edges).isEqualTo("edges");
    assertThat(DgsConstants.ARTICLESCONNECTION.PageInfo).isEqualTo("pageInfo");
  }

  @Test
  public void should_expose_comment_constants() {
    assertThat(new DgsConstants.COMMENT()).isNotNull();
    assertThat(DgsConstants.COMMENT.TYPE_NAME).isEqualTo("Comment");
    assertThat(DgsConstants.COMMENT.Id).isEqualTo("id");
    assertThat(DgsConstants.COMMENT.Author).isEqualTo("author");
    assertThat(DgsConstants.COMMENT.Article).isEqualTo("article");
    assertThat(DgsConstants.COMMENT.Body).isEqualTo("body");
    assertThat(DgsConstants.COMMENT.CreatedAt).isEqualTo("createdAt");
    assertThat(DgsConstants.COMMENT.UpdatedAt).isEqualTo("updatedAt");

    assertThat(new DgsConstants.COMMENTEDGE()).isNotNull();
    assertThat(DgsConstants.COMMENTEDGE.TYPE_NAME).isEqualTo("CommentEdge");
    assertThat(DgsConstants.COMMENTEDGE.Cursor).isEqualTo("cursor");
    assertThat(DgsConstants.COMMENTEDGE.Node).isEqualTo("node");

    assertThat(new DgsConstants.COMMENTSCONNECTION()).isNotNull();
    assertThat(DgsConstants.COMMENTSCONNECTION.TYPE_NAME).isEqualTo("CommentsConnection");
    assertThat(DgsConstants.COMMENTSCONNECTION.Edges).isEqualTo("edges");
    assertThat(DgsConstants.COMMENTSCONNECTION.PageInfo).isEqualTo("pageInfo");
  }

  @Test
  public void should_expose_deletion_status_and_page_info_constants() {
    assertThat(new DgsConstants.DELETIONSTATUS()).isNotNull();
    assertThat(DgsConstants.DELETIONSTATUS.TYPE_NAME).isEqualTo("DeletionStatus");
    assertThat(DgsConstants.DELETIONSTATUS.Success).isEqualTo("success");

    assertThat(new DgsConstants.PAGEINFO()).isNotNull();
    assertThat(DgsConstants.PAGEINFO.TYPE_NAME).isEqualTo("PageInfo");
    assertThat(DgsConstants.PAGEINFO.EndCursor).isEqualTo("endCursor");
    assertThat(DgsConstants.PAGEINFO.HasNextPage).isEqualTo("hasNextPage");
    assertThat(DgsConstants.PAGEINFO.HasPreviousPage).isEqualTo("hasPreviousPage");
    assertThat(DgsConstants.PAGEINFO.StartCursor).isEqualTo("startCursor");
  }

  @Test
  public void should_expose_profile_and_user_constants() {
    assertThat(new DgsConstants.PROFILE()).isNotNull();
    assertThat(DgsConstants.PROFILE.TYPE_NAME).isEqualTo("Profile");
    assertThat(DgsConstants.PROFILE.Username).isEqualTo("username");
    assertThat(DgsConstants.PROFILE.Bio).isEqualTo("bio");
    assertThat(DgsConstants.PROFILE.Following).isEqualTo("following");
    assertThat(DgsConstants.PROFILE.Image).isEqualTo("image");
    assertThat(DgsConstants.PROFILE.Articles).isEqualTo("articles");
    assertThat(DgsConstants.PROFILE.Favorites).isEqualTo("favorites");
    assertThat(DgsConstants.PROFILE.Feed).isEqualTo("feed");

    assertThat(new DgsConstants.USER()).isNotNull();
    assertThat(DgsConstants.USER.TYPE_NAME).isEqualTo("User");
    assertThat(DgsConstants.USER.Email).isEqualTo("email");
    assertThat(DgsConstants.USER.Profile).isEqualTo("profile");
    assertThat(DgsConstants.USER.Token).isEqualTo("token");
    assertThat(DgsConstants.USER.Username).isEqualTo("username");
  }

  @Test
  public void should_expose_error_constants() {
    assertThat(new DgsConstants.ERROR()).isNotNull();
    assertThat(DgsConstants.ERROR.TYPE_NAME).isEqualTo("Error");
    assertThat(DgsConstants.ERROR.Message).isEqualTo("message");
    assertThat(DgsConstants.ERROR.Errors).isEqualTo("errors");

    assertThat(new DgsConstants.ERRORITEM()).isNotNull();
    assertThat(DgsConstants.ERRORITEM.TYPE_NAME).isEqualTo("ErrorItem");
    assertThat(DgsConstants.ERRORITEM.Key).isEqualTo("key");
    assertThat(DgsConstants.ERRORITEM.Value).isEqualTo("value");
  }

  @Test
  public void should_expose_payload_constants() {
    assertThat(new DgsConstants.ARTICLEPAYLOAD()).isNotNull();
    assertThat(DgsConstants.ARTICLEPAYLOAD.TYPE_NAME).isEqualTo("ArticlePayload");
    assertThat(DgsConstants.ARTICLEPAYLOAD.Article).isEqualTo("article");

    assertThat(new DgsConstants.COMMENTPAYLOAD()).isNotNull();
    assertThat(DgsConstants.COMMENTPAYLOAD.TYPE_NAME).isEqualTo("CommentPayload");
    assertThat(DgsConstants.COMMENTPAYLOAD.Comment).isEqualTo("comment");

    assertThat(new DgsConstants.USERPAYLOAD()).isNotNull();
    assertThat(DgsConstants.USERPAYLOAD.TYPE_NAME).isEqualTo("UserPayload");
    assertThat(DgsConstants.USERPAYLOAD.User).isEqualTo("user");

    assertThat(new DgsConstants.PROFILEPAYLOAD()).isNotNull();
    assertThat(DgsConstants.PROFILEPAYLOAD.TYPE_NAME).isEqualTo("ProfilePayload");
    assertThat(DgsConstants.PROFILEPAYLOAD.Profile).isEqualTo("profile");
  }

  @Test
  public void should_expose_input_constants() {
    assertThat(new DgsConstants.UPDATEARTICLEINPUT()).isNotNull();
    assertThat(DgsConstants.UPDATEARTICLEINPUT.TYPE_NAME).isEqualTo("UpdateArticleInput");
    assertThat(DgsConstants.UPDATEARTICLEINPUT.Body).isEqualTo("body");
    assertThat(DgsConstants.UPDATEARTICLEINPUT.Description).isEqualTo("description");
    assertThat(DgsConstants.UPDATEARTICLEINPUT.Title).isEqualTo("title");

    assertThat(new DgsConstants.CREATEARTICLEINPUT()).isNotNull();
    assertThat(DgsConstants.CREATEARTICLEINPUT.TYPE_NAME).isEqualTo("CreateArticleInput");
    assertThat(DgsConstants.CREATEARTICLEINPUT.Body).isEqualTo("body");
    assertThat(DgsConstants.CREATEARTICLEINPUT.Description).isEqualTo("description");
    assertThat(DgsConstants.CREATEARTICLEINPUT.TagList).isEqualTo("tagList");
    assertThat(DgsConstants.CREATEARTICLEINPUT.Title).isEqualTo("title");

    assertThat(new DgsConstants.CREATEUSERINPUT()).isNotNull();
    assertThat(DgsConstants.CREATEUSERINPUT.TYPE_NAME).isEqualTo("CreateUserInput");
    assertThat(DgsConstants.CREATEUSERINPUT.Email).isEqualTo("email");
    assertThat(DgsConstants.CREATEUSERINPUT.Username).isEqualTo("username");
    assertThat(DgsConstants.CREATEUSERINPUT.Password).isEqualTo("password");

    assertThat(new DgsConstants.UPDATEUSERINPUT()).isNotNull();
    assertThat(DgsConstants.UPDATEUSERINPUT.TYPE_NAME).isEqualTo("UpdateUserInput");
    assertThat(DgsConstants.UPDATEUSERINPUT.Email).isEqualTo("email");
    assertThat(DgsConstants.UPDATEUSERINPUT.Username).isEqualTo("username");
    assertThat(DgsConstants.UPDATEUSERINPUT.Password).isEqualTo("password");
    assertThat(DgsConstants.UPDATEUSERINPUT.Image).isEqualTo("image");
    assertThat(DgsConstants.UPDATEUSERINPUT.Bio).isEqualTo("bio");
  }

  @Test
  public void should_instantiate_dgs_constants_holder() {
    assertThat(new DgsConstants()).isNotNull();
  }
}
