package io.spring.graphql;

import java.lang.String;

public class DgsConstants {
  public static final String QUERY_TYPE = "Query";

  public static class QUERY {
    public static final String TYPE_NAME = "Query";

    public static final String Article = "article";

    public static final String Articles = "articles";

    public static final String Me = "me";

    public static final String Feed = "feed";

    public static final String Profile = "profile";

    public static final String Tags = "tags";
  }

  public static class MUTATION {
    public static final String TYPE_NAME = "Mutation";

    public static final String CreateUser = "createUser";

    public static final String Login = "login";

    public static final String UpdateUser = "updateUser";

    public static final String FollowUser = "followUser";

    public static final String UnfollowUser = "unfollowUser";

    public static final String CreateArticle = "createArticle";

    public static final String UpdateArticle = "updateArticle";

    public static final String FavoriteArticle = "favoriteArticle";

    public static final String UnfavoriteArticle = "unfavoriteArticle";

    public static final String DeleteArticle = "deleteArticle";

    public static final String AddComment = "addComment";

    public static final String DeleteComment = "deleteComment";
  }

  public static class ARTICLE {
    public static final String TYPE_NAME = "Article";

    public static final String Author = "author";

    public static final String Body = "body";

    public static final String Comments = "comments";

    public static final String CreatedAt = "createdAt";

    public static final String Description = "description";

    public static final String Favorited = "favorited";

    public static final String FavoritesCount = "favoritesCount";

    public static final String Slug = "slug";

    public static final String TagList = "tagList";

    public static final String Title = "title";

    public static final String UpdatedAt = "updatedAt";
  }

  public static class ARTICLEEDGE {
    public static final String TYPE_NAME = "ArticleEdge";

    public static final String Cursor = "cursor";

    public static final String Node = "node";
  }

  public static class ARTICLESCONNECTION {
    public static final String TYPE_NAME = "ArticlesConnection";

    public static final String Edges = "edges";

    public static final String PageInfo = "pageInfo";
  }

  public static class COMMENT {
    public static final String TYPE_NAME = "Comment";

    public static final String Id = "id";

    public static final String Author = "author";

    public static final String Article = "article";

    public static final String Body = "body";

    public static final String CreatedAt = "createdAt";

    public static final String UpdatedAt = "updatedAt";
  }

  public static class COMMENTEDGE {
    public static final String TYPE_NAME = "CommentEdge";

    public static final String Cursor = "cursor";

    public static final String Node = "node";
  }

  public static class COMMENTSCONNECTION {
    public static final String TYPE_NAME = "CommentsConnection";

    public static final String Edges = "edges";

    public static final String PageInfo = "pageInfo";
  }

  public static class DELETIONSTATUS {
    public static final String TYPE_NAME = "DeletionStatus";

    public static final String Success = "success";
  }

  public static class PAGEINFO {
    public static final String TYPE_NAME = "PageInfo";

    public static final String EndCursor = "endCursor";

    public static final String HasNextPage = "hasNextPage";

    public static final String HasPreviousPage = "hasPreviousPage";

    public static final String StartCursor = "startCursor";
  }

  public static class PROFILE {
    public static final String TYPE_NAME = "Profile";

    public static final String Username = "username";

    public static final String Bio = "bio";

    public static final String Following = "following";

    public static final String Image = "image";

    public static final String Articles = "articles";

    public static final String Favorites = "favorites";

    public static final String Feed = "feed";
  }

  public static class USER {
    public static final String TYPE_NAME = "User";

    public static final String Email = "email";

    public static final String Profile = "profile";

    public static final String Token = "token";

    public static final String Username = "username";
  }

  public static class ERROR {
    public static final String TYPE_NAME = "Error";

    public static final String Message = "message";

    public static final String Errors = "errors";
  }

  public static class ERRORITEM {
    public static final String TYPE_NAME = "ErrorItem";

    public static final String Key = "key";

    public static final String Value = "value";
  }

  public static class ARTICLEPAYLOAD {
    public static final String TYPE_NAME = "ArticlePayload";

    public static final String Article = "article";
  }

  public static class COMMENTPAYLOAD {
    public static final String TYPE_NAME = "CommentPayload";

    public static final String Comment = "comment";
  }

  public static class USERPAYLOAD {
    public static final String TYPE_NAME = "UserPayload";

    public static final String User = "user";
  }

  public static class PROFILEPAYLOAD {
    public static final String TYPE_NAME = "ProfilePayload";

    public static final String Profile = "profile";
  }

  public static class UPDATEARTICLEINPUT {
    public static final String TYPE_NAME = "UpdateArticleInput";

    public static final String Body = "body";

    public static final String Description = "description";

    public static final String Title = "title";
  }

  public static class CREATEARTICLEINPUT {
    public static final String TYPE_NAME = "CreateArticleInput";

    public static final String Body = "body";

    public static final String Description = "description";

    public static final String TagList = "tagList";

    public static final String Title = "title";
  }

  public static class CREATEUSERINPUT {
    public static final String TYPE_NAME = "CreateUserInput";

    public static final String Email = "email";

    public static final String Username = "username";

    public static final String Password = "password";
  }

  public static class UPDATEUSERINPUT {
    public static final String TYPE_NAME = "UpdateUserInput";

    public static final String Email = "email";

    public static final String Username = "username";

    public static final String Password = "password";

    public static final String Image = "image";

    public static final String Bio = "bio";
  }
}
