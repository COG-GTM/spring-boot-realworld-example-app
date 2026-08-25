package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ArticlePayloadTypeTest {
  private static Article sample() {
    return Article.newBuilder().slug("how-to-train-your-dragon").title("How to train").build();
  }

  @Test
  public void should_build_with_builder() {
    Article value = sample();
    ArticlePayload payload = ArticlePayload.newBuilder().article(value).build();
    assertThat(payload.getArticle()).isSameAs(value);
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    Article value = sample();
    ArticlePayload payload = new ArticlePayload(value);
    assertThat(payload.getArticle()).isSameAs(value);
  }

  @Test
  public void should_support_no_args_constructor_and_setter() {
    ArticlePayload payload = new ArticlePayload();
    assertThat(payload.getArticle()).isNull();
    Article value = sample();
    payload.setArticle(value);
    assertThat(payload.getArticle()).isSameAs(value);
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    ArticlePayload one = new ArticlePayload(sample());
    ArticlePayload same = new ArticlePayload(sample());
    ArticlePayload different = new ArticlePayload(Article.newBuilder().slug("other").build());
    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(different).isNotEqualTo(null);
    assertThat(one.equals("not a payload")).isFalse();
    assertThat(one.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_render_field_in_to_string() {
    ArticlePayload payload = new ArticlePayload(sample());
    assertThat(payload.toString())
        .startsWith("ArticlePayload{")
        .contains("how-to-train-your-dragon")
        .endsWith("}");
  }
}
