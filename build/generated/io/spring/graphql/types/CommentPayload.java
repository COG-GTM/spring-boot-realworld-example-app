package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class CommentPayload {
  private Comment comment;

  public CommentPayload() {
  }

  public CommentPayload(Comment comment) {
    this.comment = comment;
  }

  public Comment getComment() {
    return comment;
  }

  public void setComment(Comment comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return "CommentPayload{" + "comment='" + comment + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentPayload that = (CommentPayload) o;
        return java.util.Objects.equals(comment, that.comment);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(comment);
  }

  public static io.spring.graphql.types.CommentPayload.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Comment comment;

    public CommentPayload build() {
      io.spring.graphql.types.CommentPayload result = new io.spring.graphql.types.CommentPayload();
          result.comment = this.comment;
          return result;
    }

    public io.spring.graphql.types.CommentPayload.Builder comment(Comment comment) {
      this.comment = comment;
      return this;
    }
  }
}
