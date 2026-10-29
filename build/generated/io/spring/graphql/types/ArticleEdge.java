package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class ArticleEdge {
  private String cursor;

  private Article node;

  public ArticleEdge() {
  }

  public ArticleEdge(String cursor, Article node) {
    this.cursor = cursor;
    this.node = node;
  }

  public String getCursor() {
    return cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }

  public Article getNode() {
    return node;
  }

  public void setNode(Article node) {
    this.node = node;
  }

  @Override
  public String toString() {
    return "ArticleEdge{" + "cursor='" + cursor + "'," +"node='" + node + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleEdge that = (ArticleEdge) o;
        return java.util.Objects.equals(cursor, that.cursor) &&
                            java.util.Objects.equals(node, that.node);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(cursor, node);
  }

  public static io.spring.graphql.types.ArticleEdge.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String cursor;

    private Article node;

    public ArticleEdge build() {
                  io.spring.graphql.types.ArticleEdge result = new io.spring.graphql.types.ArticleEdge();
                      result.cursor = this.cursor;
          result.node = this.node;
                      return result;
    }

    public io.spring.graphql.types.ArticleEdge.Builder cursor(String cursor) {
      this.cursor = cursor;
      return this;
    }

    public io.spring.graphql.types.ArticleEdge.Builder node(Article node) {
      this.node = node;
      return this;
    }
  }
}
