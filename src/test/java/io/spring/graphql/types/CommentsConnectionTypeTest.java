package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.relay.DefaultConnectionCursor;
import graphql.relay.DefaultPageInfo;
import graphql.relay.PageInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CommentsConnectionTypeTest {
  private PageInfo pageInfo() {
    return new DefaultPageInfo(
        new DefaultConnectionCursor("start"), new DefaultConnectionCursor("end"), true, false);
  }

  private List<CommentEdge> edges() {
    return Arrays.asList(
        new CommentEdge("start", Comment.newBuilder().id("one").build()),
        new CommentEdge("end", Comment.newBuilder().id("two").build()));
  }

  @Test
  public void should_build_connection_with_builder() {
    List<CommentEdge> edges = edges();
    PageInfo pageInfo = pageInfo();

    CommentsConnection connection =
        CommentsConnection.newBuilder().edges(edges).pageInfo(pageInfo).build();

    assertThat(connection.getEdges()).isSameAs(edges);
    assertThat(connection.getEdges())
        .extracting(edge -> edge.getNode().getId())
        .containsExactly("one", "two");
    assertThat(connection.getPageInfo()).isSameAs(pageInfo);
    assertThat(connection.getPageInfo().isHasPreviousPage()).isTrue();
  }

  @Test
  public void should_construct_connection_with_all_args_constructor() {
    CommentsConnection connection = new CommentsConnection(edges(), pageInfo());

    assertThat(connection.getEdges()).hasSize(2);
    assertThat(connection.getEdges().get(1).getCursor()).isEqualTo("end");
    assertThat(connection.getPageInfo().getEndCursor().getValue()).isEqualTo("end");
  }

  @Test
  public void should_set_fields_with_setters() {
    CommentsConnection connection = new CommentsConnection();
    assertThat(connection.getEdges()).isNull();
    assertThat(connection.getPageInfo()).isNull();

    connection.setEdges(Collections.emptyList());
    PageInfo pageInfo = pageInfo();
    connection.setPageInfo(pageInfo);

    assertThat(connection.getEdges()).isEmpty();
    assertThat(connection.getPageInfo()).isSameAs(pageInfo);
  }

  @Test
  public void should_render_fields_in_to_string() {
    CommentsConnection connection = new CommentsConnection(edges(), pageInfo());

    assertThat(connection.toString()).startsWith("CommentsConnection{").contains("one", "two");
  }

  @Test
  public void should_compare_by_value() {
    PageInfo pageInfo = pageInfo();
    CommentsConnection one = new CommentsConnection(edges(), pageInfo);
    CommentsConnection same = new CommentsConnection(edges(), pageInfo);
    CommentsConnection other = new CommentsConnection(Collections.emptyList(), pageInfo);

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals("not a connection")).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
