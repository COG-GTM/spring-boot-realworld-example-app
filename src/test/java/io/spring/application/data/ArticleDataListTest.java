package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ArticleDataListTest {

  @Test
  public void should_wrap_articles_and_total_count() {
    ArticleData first = new ArticleData();
    first.setSlug("first");
    ArticleData second = new ArticleData();
    second.setSlug("second");

    ArticleDataList list = new ArticleDataList(Arrays.asList(first, second), 42);

    assertThat(list.getArticleDatas()).containsExactly(first, second);
    assertThat(list.getCount()).isEqualTo(42);
  }

  @Test
  public void should_allow_empty_list_with_zero_count() {
    ArticleDataList list = new ArticleDataList(Collections.emptyList(), 0);

    assertThat(list.getArticleDatas()).isEmpty();
    assertThat(list.getCount()).isZero();
  }

  @Test
  public void should_keep_count_independent_of_page_size() {
    ArticleDataList list = new ArticleDataList(Arrays.asList(new ArticleData()), 100);

    assertThat(list.getArticleDatas()).hasSize(1);
    assertThat(list.getCount()).isEqualTo(100);
  }
}
