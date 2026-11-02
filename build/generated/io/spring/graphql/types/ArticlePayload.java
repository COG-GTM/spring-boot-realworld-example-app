package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class ArticlePayload {
  private Article article;

  public ArticlePayload() {
  }

  public ArticlePayload(Article article) {
    this.article = article;
  }

  public Article getArticle() {
    return article;
  }

  public void setArticle(Article article) {
    this.article = article;
  }

  @Override
  public String toString() {
    return "ArticlePayload{" + "article='" + article + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticlePayload that = (ArticlePayload) o;
        return java.util.Objects.equals(article, that.article);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(article);
  }

  public static io.spring.graphql.types.ArticlePayload.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Article article;

    public ArticlePayload build() {
      io.spring.graphql.types.ArticlePayload result = new io.spring.graphql.types.ArticlePayload();
          result.article = this.article;
          return result;
    }

    public io.spring.graphql.types.ArticlePayload.Builder article(Article article) {
      this.article = article;
      return this;
    }
  }
}
