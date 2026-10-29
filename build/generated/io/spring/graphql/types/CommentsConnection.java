package io.spring.graphql.types;

import graphql.relay.PageInfo;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class CommentsConnection {
  private List<CommentEdge> edges;

  private PageInfo pageInfo;

  public CommentsConnection() {
  }

  public CommentsConnection(List<CommentEdge> edges, PageInfo pageInfo) {
    this.edges = edges;
    this.pageInfo = pageInfo;
  }

  public List<CommentEdge> getEdges() {
    return edges;
  }

  public void setEdges(List<CommentEdge> edges) {
    this.edges = edges;
  }

  public PageInfo getPageInfo() {
    return pageInfo;
  }

  public void setPageInfo(PageInfo pageInfo) {
    this.pageInfo = pageInfo;
  }

  @Override
  public String toString() {
    return "CommentsConnection{" + "edges='" + edges + "'," +"pageInfo='" + pageInfo + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentsConnection that = (CommentsConnection) o;
        return java.util.Objects.equals(edges, that.edges) &&
                            java.util.Objects.equals(pageInfo, that.pageInfo);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(edges, pageInfo);
  }

  public static io.spring.graphql.types.CommentsConnection.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private List<CommentEdge> edges;

    private PageInfo pageInfo;

    public CommentsConnection build() {
                  io.spring.graphql.types.CommentsConnection result = new io.spring.graphql.types.CommentsConnection();
                      result.edges = this.edges;
          result.pageInfo = this.pageInfo;
                      return result;
    }

    public io.spring.graphql.types.CommentsConnection.Builder edges(List<CommentEdge> edges) {
      this.edges = edges;
      return this;
    }

    public io.spring.graphql.types.CommentsConnection.Builder pageInfo(PageInfo pageInfo) {
      this.pageInfo = pageInfo;
      return this;
    }
  }
}
