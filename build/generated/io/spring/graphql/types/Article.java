package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class Article {
  private Profile author;

  private String body;

  private CommentsConnection comments;

  private String createdAt;

  private String description;

  private boolean favorited;

  private int favoritesCount;

  private String slug;

  private List<String> tagList;

  private String title;

  private String updatedAt;

  public Article() {
  }

  public Article(Profile author, String body, CommentsConnection comments, String createdAt,
      String description, boolean favorited, int favoritesCount, String slug, List<String> tagList,
      String title, String updatedAt) {
    this.author = author;
    this.body = body;
    this.comments = comments;
    this.createdAt = createdAt;
    this.description = description;
    this.favorited = favorited;
    this.favoritesCount = favoritesCount;
    this.slug = slug;
    this.tagList = tagList;
    this.title = title;
    this.updatedAt = updatedAt;
  }

  public Profile getAuthor() {
    return author;
  }

  public void setAuthor(Profile author) {
    this.author = author;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public CommentsConnection getComments() {
    return comments;
  }

  public void setComments(CommentsConnection comments) {
    this.comments = comments;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean getFavorited() {
    return favorited;
  }

  public void setFavorited(boolean favorited) {
    this.favorited = favorited;
  }

  public int getFavoritesCount() {
    return favoritesCount;
  }

  public void setFavoritesCount(int favoritesCount) {
    this.favoritesCount = favoritesCount;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public List<String> getTagList() {
    return tagList;
  }

  public void setTagList(List<String> tagList) {
    this.tagList = tagList;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public String toString() {
    return "Article{" + "author='" + author + "'," +"body='" + body + "'," +"comments='" + comments + "'," +"createdAt='" + createdAt + "'," +"description='" + description + "'," +"favorited='" + favorited + "'," +"favoritesCount='" + favoritesCount + "'," +"slug='" + slug + "'," +"tagList='" + tagList + "'," +"title='" + title + "'," +"updatedAt='" + updatedAt + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article that = (Article) o;
        return java.util.Objects.equals(author, that.author) &&
                            java.util.Objects.equals(body, that.body) &&
                            java.util.Objects.equals(comments, that.comments) &&
                            java.util.Objects.equals(createdAt, that.createdAt) &&
                            java.util.Objects.equals(description, that.description) &&
                            favorited == that.favorited &&
                            favoritesCount == that.favoritesCount &&
                            java.util.Objects.equals(slug, that.slug) &&
                            java.util.Objects.equals(tagList, that.tagList) &&
                            java.util.Objects.equals(title, that.title) &&
                            java.util.Objects.equals(updatedAt, that.updatedAt);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(author, body, comments, createdAt, description, favorited, favoritesCount, slug, tagList, title, updatedAt);
  }

  public static io.spring.graphql.types.Article.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Profile author;

    private String body;

    private CommentsConnection comments;

    private String createdAt;

    private String description;

    private boolean favorited;

    private int favoritesCount;

    private String slug;

    private List<String> tagList;

    private String title;

    private String updatedAt;

    public Article build() {
                  io.spring.graphql.types.Article result = new io.spring.graphql.types.Article();
                      result.author = this.author;
          result.body = this.body;
          result.comments = this.comments;
          result.createdAt = this.createdAt;
          result.description = this.description;
          result.favorited = this.favorited;
          result.favoritesCount = this.favoritesCount;
          result.slug = this.slug;
          result.tagList = this.tagList;
          result.title = this.title;
          result.updatedAt = this.updatedAt;
                      return result;
    }

    public io.spring.graphql.types.Article.Builder author(Profile author) {
      this.author = author;
      return this;
    }

    public io.spring.graphql.types.Article.Builder body(String body) {
      this.body = body;
      return this;
    }

    public io.spring.graphql.types.Article.Builder comments(CommentsConnection comments) {
      this.comments = comments;
      return this;
    }

    public io.spring.graphql.types.Article.Builder createdAt(String createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public io.spring.graphql.types.Article.Builder description(String description) {
      this.description = description;
      return this;
    }

    public io.spring.graphql.types.Article.Builder favorited(boolean favorited) {
      this.favorited = favorited;
      return this;
    }

    public io.spring.graphql.types.Article.Builder favoritesCount(int favoritesCount) {
      this.favoritesCount = favoritesCount;
      return this;
    }

    public io.spring.graphql.types.Article.Builder slug(String slug) {
      this.slug = slug;
      return this;
    }

    public io.spring.graphql.types.Article.Builder tagList(List<String> tagList) {
      this.tagList = tagList;
      return this;
    }

    public io.spring.graphql.types.Article.Builder title(String title) {
      this.title = title;
      return this;
    }

    public io.spring.graphql.types.Article.Builder updatedAt(String updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }
  }
}
