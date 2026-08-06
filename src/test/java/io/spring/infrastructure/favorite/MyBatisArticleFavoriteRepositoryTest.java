package io.spring.infrastructure.favorite;

import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisArticleFavoriteRepository.class})
public class MyBatisArticleFavoriteRepositoryTest extends DbTestBase {
  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

  @Autowired
  private io.spring.infrastructure.mybatis.mapper.ArticleFavoriteMapper articleFavoriteMapper;

  @Autowired private ArticleFavoritesReadService articleFavoritesReadService;

  @Test
  public void should_save_and_fetch_articleFavorite_success() {
    ArticleFavorite articleFavorite = new ArticleFavorite("123", "456");
    articleFavoriteRepository.save(articleFavorite);
    Assertions.assertNotNull(
        articleFavoriteMapper.find(articleFavorite.getArticleId(), articleFavorite.getUserId()));
  }

  @Test
  public void should_remove_favorite_success() {
    ArticleFavorite articleFavorite = new ArticleFavorite("123", "456");
    articleFavoriteRepository.save(articleFavorite);
    articleFavoriteRepository.remove(articleFavorite);
    Assertions.assertFalse(articleFavoriteRepository.find("123", "456").isPresent());
  }

  @Test
  public void should_find_favorite_success() {
    ArticleFavorite articleFavorite = new ArticleFavorite("123", "456");
    articleFavoriteRepository.save(articleFavorite);

    Optional<ArticleFavorite> optional = articleFavoriteRepository.find("123", "456");
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get(), articleFavorite);
  }

  @Test
  public void should_get_empty_optional_when_favorite_not_exists() {
    articleFavoriteRepository.save(new ArticleFavorite("123", "456"));

    Assertions.assertFalse(articleFavoriteRepository.find("123", "another-user").isPresent());
    Assertions.assertFalse(articleFavoriteRepository.find("another-article", "456").isPresent());
  }

  @Test
  public void should_not_duplicate_favorite_when_saved_twice() {
    articleFavoriteRepository.save(new ArticleFavorite("123", "456"));
    articleFavoriteRepository.save(new ArticleFavorite("123", "456"));

    Assertions.assertEquals(1, articleFavoritesReadService.articleFavoriteCount("123"));
  }

  @Test
  public void should_only_remove_the_target_favorite() {
    articleFavoriteRepository.save(new ArticleFavorite("123", "456"));
    articleFavoriteRepository.save(new ArticleFavorite("123", "789"));

    articleFavoriteRepository.remove(new ArticleFavorite("123", "456"));

    Assertions.assertFalse(articleFavoriteRepository.find("123", "456").isPresent());
    Assertions.assertTrue(articleFavoriteRepository.find("123", "789").isPresent());
  }

  @Test
  public void should_do_nothing_when_removing_a_favorite_that_does_not_exist() {
    articleFavoriteRepository.remove(new ArticleFavorite("123", "456"));

    Assertions.assertFalse(articleFavoriteRepository.find("123", "456").isPresent());
  }
}
