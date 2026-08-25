package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArticleTypeTest {
  private static final List<String> TAGS = Arrays.asList("java", "spring");

  private Article fullArticle() {
    return new Article(
        Profile.newBuilder().username("jack").build(),
        "body",
        CommentsConnection.newBuilder().edges(Collections.emptyList()).build(),
        "2021-01-01T00:00:00Z",
        "description",
        true,
        3,
        "a-title",
        TAGS,
        "a title",
        "2021-01-02T00:00:00Z");
  }

  @Test
  public void should_build_article_with_all_fields() {
    Profile author = Profile.newBuilder().username("jack").build();
    CommentsConnection comments = CommentsConnection.newBuilder().build();

    Article article =
        Article.newBuilder()
            .author(author)
            .body("body")
            .comments(comments)
            .createdAt("2021-01-01T00:00:00Z")
            .description("description")
            .favorited(true)
            .favoritesCount(3)
            .slug("a-title")
            .tagList(TAGS)
            .title("a title")
            .updatedAt("2021-01-02T00:00:00Z")
            .build();

    assertThat(article.getAuthor()).isSameAs(author);
    assertThat(article.getBody()).isEqualTo("body");
    assertThat(article.getComments()).isSameAs(comments);
    assertThat(article.getCreatedAt()).isEqualTo("2021-01-01T00:00:00Z");
    assertThat(article.getDescription()).isEqualTo("description");
    assertThat(article.getFavorited()).isTrue();
    assertThat(article.getFavoritesCount()).isEqualTo(3);
    assertThat(article.getSlug()).isEqualTo("a-title");
    assertThat(article.getTagList()).containsExactly("java", "spring");
    assertThat(article.getTitle()).isEqualTo("a title");
    assertThat(article.getUpdatedAt()).isEqualTo("2021-01-02T00:00:00Z");
  }

  @Test
  public void should_construct_article_with_all_args_constructor() {
    Article article = fullArticle();

    assertThat(article.getAuthor().getUsername()).isEqualTo("jack");
    assertThat(article.getBody()).isEqualTo("body");
    assertThat(article.getComments().getEdges()).isEmpty();
    assertThat(article.getCreatedAt()).isEqualTo("2021-01-01T00:00:00Z");
    assertThat(article.getDescription()).isEqualTo("description");
    assertThat(article.getFavorited()).isTrue();
    assertThat(article.getFavoritesCount()).isEqualTo(3);
    assertThat(article.getSlug()).isEqualTo("a-title");
    assertThat(article.getTagList()).isEqualTo(TAGS);
    assertThat(article.getTitle()).isEqualTo("a title");
    assertThat(article.getUpdatedAt()).isEqualTo("2021-01-02T00:00:00Z");
  }

  @Test
  public void should_set_fields_with_setters() {
    Article article = new Article();
    assertThat(article.getTitle()).isNull();
    assertThat(article.getFavorited()).isFalse();
    assertThat(article.getFavoritesCount()).isZero();

    Profile author = Profile.newBuilder().username("jill").build();
    CommentsConnection comments = CommentsConnection.newBuilder().build();
    article.setAuthor(author);
    article.setBody("new body");
    article.setComments(comments);
    article.setCreatedAt("2022-01-01T00:00:00Z");
    article.setDescription("new description");
    article.setFavorited(true);
    article.setFavoritesCount(7);
    article.setSlug("new-title");
    article.setTagList(TAGS);
    article.setTitle("new title");
    article.setUpdatedAt("2022-01-02T00:00:00Z");

    assertThat(article.getAuthor()).isSameAs(author);
    assertThat(article.getBody()).isEqualTo("new body");
    assertThat(article.getComments()).isSameAs(comments);
    assertThat(article.getCreatedAt()).isEqualTo("2022-01-01T00:00:00Z");
    assertThat(article.getDescription()).isEqualTo("new description");
    assertThat(article.getFavorited()).isTrue();
    assertThat(article.getFavoritesCount()).isEqualTo(7);
    assertThat(article.getSlug()).isEqualTo("new-title");
    assertThat(article.getTagList()).containsExactly("java", "spring");
    assertThat(article.getTitle()).isEqualTo("new title");
    assertThat(article.getUpdatedAt()).isEqualTo("2022-01-02T00:00:00Z");
  }

  @Test
  public void should_render_fields_in_to_string() {
    assertThat(fullArticle().toString())
        .startsWith("Article{")
        .contains("title='a title'")
        .contains("slug='a-title'")
        .contains("favorited='true'")
        .contains("favoritesCount='3'")
        .contains("tagList='[java, spring]'");
  }

  @Test
  public void should_compare_by_value() {
    Article one = fullArticle();
    Article same = fullArticle();
    Article other = fullArticle();
    other.setFavoritesCount(99);

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals("not an article")).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
