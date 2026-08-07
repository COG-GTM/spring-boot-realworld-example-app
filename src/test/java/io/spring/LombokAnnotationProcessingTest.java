package io.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.article.NewArticleParam;
import io.spring.application.data.ArticleData;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class LombokAnnotationProcessingTest {

  @Test
  public void builder_and_getters_are_generated() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    assertThat(param.getTitle(), is("title"));
    assertThat(param.getDescription(), is("desc"));
    assertThat(param.getBody(), is("body"));
    assertThat(param.getTagList(), is(Arrays.asList("java", "spring")));
  }

  @Test
  public void data_generates_constructors_accessors_and_value_semantics() {
    DateTime now = new DateTime();
    ArticleData first =
        new ArticleData(
            "id",
            "slug",
            "title",
            "desc",
            "body",
            false,
            0,
            now,
            now,
            Collections.emptyList(),
            null);
    ArticleData second = new ArticleData();
    second.setId("id");
    second.setSlug("slug");
    second.setTitle("title");
    second.setDescription("desc");
    second.setBody("body");
    second.setCreatedAt(now);
    second.setUpdatedAt(now);
    second.setTagList(Collections.emptyList());

    assertThat(second, is(first));
    assertThat(second.hashCode(), is(first.hashCode()));
    assertThat(first.toString().contains("slug"), is(true));
  }

  @Test
  public void equals_and_hash_code_honour_explicit_field_selection() {
    User user = new User("email@test.com", "username", "123", "bio", "image");
    User otherId = new User("email@test.com", "username", "123", "bio", "image");

    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(otherId.getId(), not(user.getId()));
    assertThat(otherId, not(user));
  }
}
