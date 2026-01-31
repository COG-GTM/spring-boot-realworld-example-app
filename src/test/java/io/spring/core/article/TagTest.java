package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_create_tag_with_name() {
    Tag tag = new Tag("java");
    
    assertThat(tag.getId(), notNullValue());
    assertThat(tag.getName(), is("java"));
  }

  @Test
  public void should_generate_unique_id_for_each_tag() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");
    
    assertThat(tag1.getId(), not(tag2.getId()));
  }

  @Test
  public void should_have_equality_based_on_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    Tag tag3 = new Tag("spring");
    
    assertThat(tag1.equals(tag2), is(true));
    assertThat(tag1.equals(tag3), is(false));
  }

  @Test
  public void should_have_same_hashcode_for_same_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    
    assertThat(tag1.hashCode(), is(tag2.hashCode()));
  }

  @Test
  public void should_allow_setting_name() {
    Tag tag = new Tag("java");
    tag.setName("spring");
    
    assertThat(tag.getName(), is("spring"));
  }

  @Test
  public void should_allow_setting_id() {
    Tag tag = new Tag("java");
    String originalId = tag.getId();
    tag.setId("custom-id");
    
    assertThat(tag.getId(), is("custom-id"));
    assertThat(tag.getId(), not(originalId));
  }
}
