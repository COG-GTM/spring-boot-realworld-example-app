package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleEdgeTypeTest {
  @Test
  public void should_build_edge_with_builder() {
    Article node = Article.newBuilder().slug("a-title").build();

    ArticleEdge edge = ArticleEdge.newBuilder().cursor("cursor").node(node).build();

    assertThat(edge.getCursor()).isEqualTo("cursor");
    assertThat(edge.getNode()).isSameAs(node);
  }

  @Test
  public void should_construct_edge_with_all_args_constructor() {
    Article node = Article.newBuilder().slug("a-title").build();

    ArticleEdge edge = new ArticleEdge("cursor", node);

    assertThat(edge.getCursor()).isEqualTo("cursor");
    assertThat(edge.getNode()).isSameAs(node);
  }

  @Test
  public void should_set_fields_with_setters() {
    ArticleEdge edge = new ArticleEdge();
    assertThat(edge.getCursor()).isNull();
    assertThat(edge.getNode()).isNull();

    Article node = Article.newBuilder().slug("other").build();
    edge.setCursor("c");
    edge.setNode(node);

    assertThat(edge.getCursor()).isEqualTo("c");
    assertThat(edge.getNode()).isSameAs(node);
  }

  @Test
  public void should_render_fields_in_to_string() {
    ArticleEdge edge = new ArticleEdge("cursor", Article.newBuilder().slug("a-title").build());

    assertThat(edge.toString())
        .startsWith("ArticleEdge{")
        .contains("cursor='cursor'")
        .contains("a-title");
  }

  @Test
  public void should_compare_by_value() {
    Article node = Article.newBuilder().slug("a-title").build();
    ArticleEdge one = new ArticleEdge("cursor", node);
    ArticleEdge same = new ArticleEdge("cursor", Article.newBuilder().slug("a-title").build());
    ArticleEdge other = new ArticleEdge("other-cursor", node);

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals("not an edge")).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
