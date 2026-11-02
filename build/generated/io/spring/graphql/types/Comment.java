package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class Comment {
  private String id;

  private Profile author;

  private Article article;

  private String body;

  private String createdAt;

  private String updatedAt;

  public Comment() {
  }

  public Comment(String id, Profile author, Article article, String body, String createdAt,
      String updatedAt) {
    this.id = id;
    this.author = author;
    this.article = article;
    this.body = body;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Profile getAuthor() {
    return author;
  }

  public void setAuthor(Profile author) {
    this.author = author;
  }

  public Article getArticle() {
    return article;
  }

  public void setArticle(Article article) {
    this.article = article;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public String toString() {
    return "Comment{" + "id='" + id + "'," +"author='" + author + "'," +"article='" + article + "'," +"body='" + body + "'," +"createdAt='" + createdAt + "'," +"updatedAt='" + updatedAt + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment that = (Comment) o;
        return java.util.Objects.equals(id, that.id) &&
                            java.util.Objects.equals(author, that.author) &&
                            java.util.Objects.equals(article, that.article) &&
                            java.util.Objects.equals(body, that.body) &&
                            java.util.Objects.equals(createdAt, that.createdAt) &&
                            java.util.Objects.equals(updatedAt, that.updatedAt);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, author, article, body, createdAt, updatedAt);
  }

  public static io.spring.graphql.types.Comment.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String id;

    private Profile author;

    private Article article;

    private String body;

    private String createdAt;

    private String updatedAt;

    public Comment build() {
                  io.spring.graphql.types.Comment result = new io.spring.graphql.types.Comment();
                      result.id = this.id;
          result.author = this.author;
          result.article = this.article;
          result.body = this.body;
          result.createdAt = this.createdAt;
          result.updatedAt = this.updatedAt;
                      return result;
    }

    public io.spring.graphql.types.Comment.Builder id(String id) {
      this.id = id;
      return this;
    }

    public io.spring.graphql.types.Comment.Builder author(Profile author) {
      this.author = author;
      return this;
    }

    public io.spring.graphql.types.Comment.Builder article(Article article) {
      this.article = article;
      return this;
    }

    public io.spring.graphql.types.Comment.Builder body(String body) {
      this.body = body;
      return this;
    }

    public io.spring.graphql.types.Comment.Builder createdAt(String createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public io.spring.graphql.types.Comment.Builder updatedAt(String updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }
  }
}
