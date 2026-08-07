package io.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.article.NewArticleParam;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    ProfileData author = new ProfileData("authorId", "author", "bio", "image", true);
    ArticleData first =
        new ArticleData(
            "id",
            "slug",
            "title",
            "desc",
            "body",
            true,
            3,
            now,
            now,
            Collections.singletonList("java"),
            author);
    ArticleData second = new ArticleData();
    second.setId("id");
    second.setSlug("slug");
    second.setTitle("title");
    second.setDescription("desc");
    second.setBody("body");
    second.setFavorited(true);
    second.setFavoritesCount(3);
    second.setCreatedAt(now);
    second.setUpdatedAt(now);
    second.setTagList(Collections.singletonList("java"));
    second.setProfileData(author);

    assertThat(second, is(first));
    assertThat(second.hashCode(), is(first.hashCode()));
    assertThat(first.toString().contains("slug"), is(true));
  }

  @Test
  public void equals_and_hash_code_honour_explicit_field_selection() {
    User user = new User("email@test.com", "username", "123", "bio", "image");
    User sameIdOnly = new User("other@test.com", "other", "456", "otherBio", "otherImage");
    ReflectionTestUtils.setField(sameIdOnly, "id", user.getId());
    User differentId = new User("email@test.com", "username", "123", "bio", "image");

    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(sameIdOnly, is(user));
    assertThat(sameIdOnly.hashCode(), is(user.hashCode()));
    assertThat(sameIdOnly.getEmail(), not(user.getEmail()));
    assertThat(differentId, not(user));
  }
}
