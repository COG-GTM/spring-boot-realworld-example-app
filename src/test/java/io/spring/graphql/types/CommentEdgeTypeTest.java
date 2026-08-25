package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CommentEdgeTypeTest {
  @Test
  public void should_build_edge_with_builder() {
    Comment node = Comment.newBuilder().id("comment-id").build();

    CommentEdge edge = CommentEdge.newBuilder().cursor("cursor").node(node).build();

    assertThat(edge.getCursor()).isEqualTo("cursor");
    assertThat(edge.getNode()).isSameAs(node);
  }

  @Test
  public void should_construct_edge_with_all_args_constructor() {
    Comment node = Comment.newBuilder().id("comment-id").build();

    CommentEdge edge = new CommentEdge("cursor", node);

    assertThat(edge.getCursor()).isEqualTo("cursor");
    assertThat(edge.getNode()).isSameAs(node);
  }

  @Test
  public void should_set_fields_with_setters() {
    CommentEdge edge = new CommentEdge();
    assertThat(edge.getCursor()).isNull();
    assertThat(edge.getNode()).isNull();

    Comment node = Comment.newBuilder().id("other-id").build();
    edge.setCursor("c");
    edge.setNode(node);

    assertThat(edge.getCursor()).isEqualTo("c");
    assertThat(edge.getNode()).isSameAs(node);
  }

  @Test
  public void should_render_fields_in_to_string() {
    CommentEdge edge = new CommentEdge("cursor", Comment.newBuilder().id("comment-id").build());

    assertThat(edge.toString())
        .startsWith("CommentEdge{")
        .contains("cursor='cursor'")
        .contains("comment-id");
  }

  @Test
  public void should_compare_by_value() {
    Comment node = Comment.newBuilder().id("comment-id").build();
    CommentEdge one = new CommentEdge("cursor", node);
    CommentEdge same = new CommentEdge("cursor", Comment.newBuilder().id("comment-id").build());
    CommentEdge other = new CommentEdge("cursor", Comment.newBuilder().id("another").build());

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals("not an edge")).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
