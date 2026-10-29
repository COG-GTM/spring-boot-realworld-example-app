package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class CommentEdge {
  private String cursor;

  private Comment node;

  public CommentEdge() {
  }

  public CommentEdge(String cursor, Comment node) {
    this.cursor = cursor;
    this.node = node;
  }

  public String getCursor() {
    return cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }

  public Comment getNode() {
    return node;
  }

  public void setNode(Comment node) {
    this.node = node;
  }

  @Override
  public String toString() {
    return "CommentEdge{" + "cursor='" + cursor + "'," +"node='" + node + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentEdge that = (CommentEdge) o;
        return java.util.Objects.equals(cursor, that.cursor) &&
                            java.util.Objects.equals(node, that.node);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(cursor, node);
  }

  public static io.spring.graphql.types.CommentEdge.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String cursor;

    private Comment node;

    public CommentEdge build() {
                  io.spring.graphql.types.CommentEdge result = new io.spring.graphql.types.CommentEdge();
                      result.cursor = this.cursor;
          result.node = this.node;
                      return result;
    }

    public io.spring.graphql.types.CommentEdge.Builder cursor(String cursor) {
      this.cursor = cursor;
      return this;
    }

    public io.spring.graphql.types.CommentEdge.Builder node(Comment node) {
      this.node = node;
      return this;
    }
  }
}
