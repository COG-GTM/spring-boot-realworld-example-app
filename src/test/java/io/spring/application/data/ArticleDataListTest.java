package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleDataListTest {

  @Test
  public void should_create_article_data_list_with_articles() {
    DateTime now = DateTime.now();
    ProfileData author = new ProfileData("author-id", "author", "bio", "image", false);

    ArticleData article1 =
        new ArticleData(
            "id1",
            "slug1",
            "Title 1",
            "Desc 1",
            "Body 1",
            false,
            0,
            now,
            now,
            Arrays.asList("tag1"),
            author);

    ArticleData article2 =
        new ArticleData(
            "id2",
            "slug2",
            "Title 2",
            "Desc 2",
            "Body 2",
            true,
            5,
            now,
            now,
            Arrays.asList("tag2"),
            author);

    List<ArticleData> articles = Arrays.asList(article1, article2);
    ArticleDataList articleDataList = new ArticleDataList(articles, 2);

    assertThat(articleDataList.getArticleDatas(), is(articles));
    assertThat(articleDataList.getCount(), is(2));
  }

  @Test
  public void should_create_empty_article_data_list() {
    ArticleDataList articleDataList = new ArticleDataList(Collections.emptyList(), 0);

    assertThat(articleDataList.getArticleDatas().isEmpty(), is(true));
    assertThat(articleDataList.getCount(), is(0));
  }

  @Test
  public void should_handle_count_different_from_list_size() {
    DateTime now = DateTime.now();
    ProfileData author = new ProfileData("author-id", "author", "bio", "image", false);

    ArticleData article =
        new ArticleData(
            "id1",
            "slug1",
            "Title 1",
            "Desc 1",
            "Body 1",
            false,
            0,
            now,
            now,
            Arrays.asList("tag1"),
            author);

    List<ArticleData> articles = Arrays.asList(article);
    ArticleDataList articleDataList = new ArticleDataList(articles, 100);

    assertThat(articleDataList.getArticleDatas().size(), is(1));
    assertThat(articleDataList.getCount(), is(100));
  }
}
