package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTypeTest {
  private Comment fullComment() {
    return new Comment(
        "comment-id",
        Profile.newBuilder().username("jack").build(),
        Article.newBuilder().slug("a-title").build(),
        "nice article",
        "2021-01-01T00:00:00Z",
        "2021-01-02T00:00:00Z");
  }

  @Test
  public void should_build_comment_with_all_fields() {
    Profile author = Profile.newBuilder().username("jack").build();
    Article article = Article.newBuilder().slug("a-title").build();

    Comment comment =
        Comment.newBuilder()
            .id("comment-id")
            .author(author)
            .article(article)
            .body("nice article")
            .createdAt("2021-01-01T00:00:00Z")
            .updatedAt("2021-01-02T00:00:00Z")
            .build();

    assertThat(comment.getId()).isEqualTo("comment-id");
    assertThat(comment.getAuthor()).isSameAs(author);
    assertThat(comment.getArticle()).isSameAs(article);
    assertThat(comment.getBody()).isEqualTo("nice article");
    assertThat(comment.getCreatedAt()).isEqualTo("2021-01-01T00:00:00Z");
    assertThat(comment.getUpdatedAt()).isEqualTo("2021-01-02T00:00:00Z");
  }

  @Test
  public void should_construct_comment_with_all_args_constructor() {
    Comment comment = fullComment();

    assertThat(comment.getId()).isEqualTo("comment-id");
    assertThat(comment.getAuthor().getUsername()).isEqualTo("jack");
    assertThat(comment.getArticle().getSlug()).isEqualTo("a-title");
    assertThat(comment.getBody()).isEqualTo("nice article");
    assertThat(comment.getCreatedAt()).isEqualTo("2021-01-01T00:00:00Z");
    assertThat(comment.getUpdatedAt()).isEqualTo("2021-01-02T00:00:00Z");
  }

  @Test
  public void should_set_fields_with_setters() {
    Comment comment = new Comment();
    assertThat(comment.getId()).isNull();

    Profile author = Profile.newBuilder().username("jill").build();
    Article article = Article.newBuilder().slug("other").build();
    comment.setId("other-id");
    comment.setAuthor(author);
    comment.setArticle(article);
    comment.setBody("body");
    comment.setCreatedAt("2022-01-01T00:00:00Z");
    comment.setUpdatedAt("2022-01-02T00:00:00Z");

    assertThat(comment.getId()).isEqualTo("other-id");
    assertThat(comment.getAuthor()).isSameAs(author);
    assertThat(comment.getArticle()).isSameAs(article);
    assertThat(comment.getBody()).isEqualTo("body");
    assertThat(comment.getCreatedAt()).isEqualTo("2022-01-01T00:00:00Z");
    assertThat(comment.getUpdatedAt()).isEqualTo("2022-01-02T00:00:00Z");
  }

  @Test
  public void should_render_fields_in_to_string() {
    assertThat(fullComment().toString())
        .startsWith("Comment{")
        .contains("id='comment-id'")
        .contains("body='nice article'")
        .contains("createdAt='2021-01-01T00:00:00Z'");
  }

  @Test
  public void should_compare_by_value() {
    Comment one = fullComment();
    Comment same = fullComment();
    Comment other = fullComment();
    other.setBody("different");

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals("not a comment")).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
