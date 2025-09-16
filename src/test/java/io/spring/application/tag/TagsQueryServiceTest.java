package io.spring.application.tag;

import io.spring.application.TagsQueryService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({TagsQueryService.class, MyBatisArticleRepository.class, MyBatisUserRepository.class})
public class TagsQueryServiceTest extends DbTestBase {
  @Autowired private TagsQueryService tagsQueryService;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private UserRepository userRepository;

  @Test
  public void should_get_all_tags() {
    User user = new User("test@example.com", "testuser", "123", "", "");
    userRepository.save(user);
    
    articleRepository.save(new Article("test", "test", "test", Arrays.asList("java"), user.getId()));
    Assertions.assertTrue(tagsQueryService.allTags().contains("java"));
  }
}
