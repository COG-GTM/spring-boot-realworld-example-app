package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.relay.DefaultConnectionCursor;
import graphql.relay.DefaultPageInfo;
import graphql.relay.PageInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArticlesConnectionTypeTest {
  private PageInfo pageInfo() {
    return new DefaultPageInfo(
        new DefaultConnectionCursor("start"), new DefaultConnectionCursor("end"), false, true);
  }

  private List<ArticleEdge> edges() {
    return Arrays.asList(
        new ArticleEdge("start", Article.newBuilder().slug("first").build()),
        new ArticleEdge("end", Article.newBuilder().slug("second").build()));
  }

  @Test
  public void should_build_connection_with_builder() {
    List<ArticleEdge> edges = edges();
    PageInfo pageInfo = pageInfo();

    ArticlesConnection connection =
        ArticlesConnection.newBuilder().edges(edges).pageInfo(pageInfo).build();

    assertThat(connection.getEdges()).isSameAs(edges);
    assertThat(connection.getEdges())
        .extracting(ArticleEdge::getCursor)
        .containsExactly("start", "end");
    assertThat(connection.getPageInfo()).isSameAs(pageInfo);
    assertThat(connection.getPageInfo().isHasNextPage()).isTrue();
  }

  @Test
  public void should_construct_connection_with_all_args_constructor() {
    List<ArticleEdge> edges = edges();

    ArticlesConnection connection = new ArticlesConnection(edges, pageInfo());

    assertThat(connection.getEdges()).hasSize(2);
    assertThat(connection.getEdges().get(0).getNode().getSlug()).isEqualTo("first");
    assertThat(connection.getPageInfo().getStartCursor().getValue()).isEqualTo("start");
  }

  @Test
  public void should_set_fields_with_setters() {
    ArticlesConnection connection = new ArticlesConnection();
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
    ArticlesConnection connection = new ArticlesConnection(edges(), pageInfo());

    assertThat(connection.toString()).startsWith("ArticlesConnection{").contains("first", "second");
  }

  @Test
  public void should_compare_by_value() {
    PageInfo pageInfo = pageInfo();
    ArticlesConnection one = new ArticlesConnection(edges(), pageInfo);
    ArticlesConnection same = new ArticlesConnection(edges(), pageInfo);
    ArticlesConnection other = new ArticlesConnection(Collections.emptyList(), pageInfo);

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals("not a connection")).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
