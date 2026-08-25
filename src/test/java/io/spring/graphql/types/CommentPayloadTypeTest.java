package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CommentPayloadTypeTest {
  private static Comment sample() {
    return Comment.newBuilder().id("comment-id").body("nice article").build();
  }

  @Test
  public void should_build_with_builder() {
    Comment value = sample();
    CommentPayload payload = CommentPayload.newBuilder().comment(value).build();
    assertThat(payload.getComment()).isSameAs(value);
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    Comment value = sample();
    CommentPayload payload = new CommentPayload(value);
    assertThat(payload.getComment()).isSameAs(value);
  }

  @Test
  public void should_support_no_args_constructor_and_setter() {
    CommentPayload payload = new CommentPayload();
    assertThat(payload.getComment()).isNull();
    Comment value = sample();
    payload.setComment(value);
    assertThat(payload.getComment()).isSameAs(value);
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    CommentPayload one = new CommentPayload(sample());
    CommentPayload same = new CommentPayload(sample());
    CommentPayload different = new CommentPayload(Comment.newBuilder().id("other-id").build());
    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(different).isNotEqualTo(null);
    assertThat(one.equals("not a payload")).isFalse();
    assertThat(one.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_render_field_in_to_string() {
    CommentPayload payload = new CommentPayload(sample());
    assertThat(payload.toString())
        .startsWith("CommentPayload{")
        .contains("comment-id")
        .endsWith("}");
  }
}
