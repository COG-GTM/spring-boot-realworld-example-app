package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ArticleSearchQueryTest {

  @Test
  public void should_quote_and_prefix_each_term() {
    assertEquals(
        Optional.of("\"spring\"* \"boot\"*"), ArticleSearchQuery.toMatchExpression("Spring Boot"));
  }

  @Test
  public void should_strip_fts_operators_and_punctuation() {
    assertEquals(
        Optional.of("\"title\"* \"java\"* \"or\"* \"not\"* \"x\"*"),
        ArticleSearchQuery.toMatchExpression("title:java\" OR -NOT (x*)"));
  }

  @Test
  public void should_deduplicate_and_cap_terms() {
    assertEquals(Optional.of("\"a\"*"), ArticleSearchQuery.toMatchExpression("a A a"));
    String many = "t1 t2 t3 t4 t5 t6 t7 t8 t9 t10 t11 t12";
    assertEquals(
        ArticleSearchQuery.MAX_TERMS,
        ArticleSearchQuery.toMatchExpression(many).get().split(" ").length);
  }

  @Test
  public void should_keep_unicode_letters() {
    assertEquals(Optional.of("\"café\"*"), ArticleSearchQuery.toMatchExpression("Café!"));
  }

  @Test
  public void should_return_empty_for_blank_or_symbol_only_input() {
    assertFalse(ArticleSearchQuery.toMatchExpression(null).isPresent());
    assertFalse(ArticleSearchQuery.toMatchExpression("   ").isPresent());
    assertFalse(ArticleSearchQuery.toMatchExpression("\"*()-:").isPresent());
  }
}
