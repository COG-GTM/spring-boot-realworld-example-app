package io.spring.application.article;

import static java.util.stream.Collectors.toList;

import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  ArticleQueryService.class,
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class
})
public class ArticleSearchServiceTest extends DbTestBase {
  @Autowired private ArticleQueryService queryService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("search@test.com", "searcher", "123", "", "");
    userRepository.save(user);
  }

  private Article save(String title, String description, String body, DateTime createdAt) {
    Article article =
        new Article(title, description, body, Collections.emptyList(), user.getId(), createdAt);
    articleRepository.save(article);
    return article;
  }

  private List<String> ids(ArticleDataList list) {
    return list.getArticleDatas().stream().map(ArticleData::getId).collect(toList());
  }

  @Test
  public void should_rank_title_match_above_description_above_body() {
    DateTime now = new DateTime();
    Article inBody = save("Weekend notes", "misc", "I tried kotlin coroutines", now);
    Article inTitle = save("Kotlin in production", "misc", "lessons learned", now.minusDays(2));
    Article inDescription = save("Language tour", "a kotlin primer", "hello", now.minusDays(1));
    save("Unrelated", "nothing", "here", now);

    ArticleDataList result = queryService.searchArticles("kotlin", new Page(), null);

    Assertions.assertEquals(3, result.getCount());
    Assertions.assertEquals(
        Arrays.asList(inTitle.getId(), inDescription.getId(), inBody.getId()), ids(result));
  }

  @Test
  public void should_break_score_ties_by_recency() {
    DateTime now = new DateTime();
    Article older = save("Rust tips one", "d", "b", now.minusDays(3));
    Article newer = save("Rust tips two", "d", "b", now);

    ArticleDataList result = queryService.searchArticles("rust", new Page(), null);

    Assertions.assertEquals(Arrays.asList(newer.getId(), older.getId()), ids(result));
  }

  @Test
  public void should_require_all_terms_and_support_prefix_and_stemming() {
    Article both = save("Testing Spring applications", "d", "with mocks", new DateTime());
    save("Spring gardening", "d", "flowers", new DateTime());

    Assertions.assertEquals(
        Collections.singletonList(both.getId()),
        ids(queryService.searchArticles("spring test", new Page(), null)));
    Assertions.assertEquals(
        Collections.singletonList(both.getId()),
        ids(queryService.searchArticles("applic", new Page(), null)));
    Assertions.assertEquals(
        Collections.singletonList(both.getId()),
        ids(queryService.searchArticles("mock", new Page(), null)));
  }

  @Test
  public void should_paginate_with_total_count() {
    DateTime now = new DateTime();
    for (int i = 0; i < 5; i++) {
      save("Paging article " + i, "d", "b", now.minusMinutes(i));
    }

    ArticleDataList first = queryService.searchArticles("paging", new Page(0, 2), null);
    ArticleDataList third = queryService.searchArticles("paging", new Page(4, 2), null);
    ArticleDataList beyond = queryService.searchArticles("paging", new Page(10, 2), null);

    Assertions.assertEquals(5, first.getCount());
    Assertions.assertEquals(2, first.getArticleDatas().size());
    Assertions.assertEquals("Paging article 0", first.getArticleDatas().get(0).getTitle());
    Assertions.assertEquals(1, third.getArticleDatas().size());
    Assertions.assertEquals("Paging article 4", third.getArticleDatas().get(0).getTitle());
    Assertions.assertEquals(5, beyond.getCount());
    Assertions.assertTrue(beyond.getArticleDatas().isEmpty());
  }

  @Test
  public void should_reflect_updates_and_deletes_in_index() {
    Article article = save("Original haskell title", "d", "b", new DateTime());

    article.update("Now about elixir", "", "");
    articleRepository.save(article);
    Assertions.assertEquals(0, queryService.searchArticles("haskell", new Page(), null).getCount());
    Assertions.assertEquals(1, queryService.searchArticles("elixir", new Page(), null).getCount());

    articleRepository.remove(article);
    Assertions.assertEquals(0, queryService.searchArticles("elixir", new Page(), null).getCount());
  }

  @Test
  public void should_fill_favorite_info_for_current_user() {
    Article article = save("Favorite golang article", "d", "b", new DateTime());
    articleFavoriteRepository.save(new ArticleFavorite(article.getId(), user.getId()));

    ArticleDataList result = queryService.searchArticles("golang", new Page(), user);

    ArticleData data = result.getArticleDatas().get(0);
    Assertions.assertTrue(data.isFavorited());
    Assertions.assertEquals(1, data.getFavoritesCount());
  }

  @Test
  public void should_return_empty_for_unsearchable_query() {
    save("Anything", "d", "b", new DateTime());

    ArticleDataList result = queryService.searchArticles("\"*:()", new Page(), null);

    Assertions.assertEquals(0, result.getCount());
    Assertions.assertTrue(result.getArticleDatas().isEmpty());
  }
}
