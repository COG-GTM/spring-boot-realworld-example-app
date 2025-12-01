package io.spring.infrastructure.tag;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.mybatis.mapper.ArticleMapper;
import io.spring.infrastructure.mybatis.readservice.TagReadService;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisArticleRepository.class, MyBatisUserRepository.class})
public class MyBatisTagRepositoryTest extends DbTestBase {

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleMapper articleMapper;

  @Autowired private TagReadService tagReadService;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("tagtest@example.com", "taguser", "password", "bio", "image");
    userRepository.save(user);
  }

  @Test
  public void should_create_tags_when_saving_article() {
    Article article =
        new Article("Tag Test Article", "Description", "Body", Arrays.asList("java", "spring"), user.getId());
    articleRepository.save(article);

    Tag javaTag = articleMapper.findTag("java");
    Tag springTag = articleMapper.findTag("spring");

    Assertions.assertNotNull(javaTag);
    Assertions.assertNotNull(springTag);
    Assertions.assertEquals("java", javaTag.getName());
    Assertions.assertEquals("spring", springTag.getName());
  }

  @Test
  public void should_find_existing_tag_by_name() {
    Article article =
        new Article("First Article", "Description", "Body", Arrays.asList("existingtag"), user.getId());
    articleRepository.save(article);

    Tag foundTag = articleMapper.findTag("existingtag");

    Assertions.assertNotNull(foundTag);
    Assertions.assertEquals("existingtag", foundTag.getName());
  }

  @Test
  public void should_return_null_for_nonexistent_tag() {
    Tag foundTag = articleMapper.findTag("nonexistenttag");

    Assertions.assertNull(foundTag);
  }

  @Test
  public void should_list_all_tags() {
    Article article1 =
        new Article("Article 1", "Description", "Body", Arrays.asList("tag1", "tag2"), user.getId());
    articleRepository.save(article1);

    Article article2 =
        new Article("Article 2", "Description", "Body", Arrays.asList("tag3", "tag4"), user.getId());
    articleRepository.save(article2);

    List<String> allTags = tagReadService.all();

    Assertions.assertNotNull(allTags);
    Assertions.assertTrue(allTags.contains("tag1"));
    Assertions.assertTrue(allTags.contains("tag2"));
    Assertions.assertTrue(allTags.contains("tag3"));
    Assertions.assertTrue(allTags.contains("tag4"));
  }

  @Test
  public void should_reuse_existing_tag_for_multiple_articles() {
    Article article1 =
        new Article("Article with Java", "Description", "Body", Arrays.asList("sharedtag"), user.getId());
    articleRepository.save(article1);

    Tag tagAfterFirstArticle = articleMapper.findTag("sharedtag");
    String firstTagId = tagAfterFirstArticle.getId();

    Article article2 =
        new Article("Another Article with Java", "Description", "Body", Arrays.asList("sharedtag"), user.getId());
    articleRepository.save(article2);

    Tag tagAfterSecondArticle = articleMapper.findTag("sharedtag");

    Assertions.assertEquals(firstTagId, tagAfterSecondArticle.getId());
  }

  @Test
  public void should_handle_article_with_no_tags() {
    Article article =
        new Article("No Tags Article", "Description", "Body", Arrays.asList(), user.getId());
    articleRepository.save(article);

    Article fetched = articleRepository.findById(article.getId()).orElse(null);

    Assertions.assertNotNull(fetched);
    Assertions.assertTrue(fetched.getTags().isEmpty());
  }

  @Test
  public void should_handle_article_with_single_tag() {
    Article article =
        new Article("Single Tag Article", "Description", "Body", Arrays.asList("singletag"), user.getId());
    articleRepository.save(article);

    Article fetched = articleRepository.findById(article.getId()).orElse(null);

    Assertions.assertNotNull(fetched);
    Assertions.assertEquals(1, fetched.getTags().size());
    Assertions.assertTrue(fetched.getTags().contains(new Tag("singletag")));
  }

  @Test
  public void should_handle_article_with_multiple_tags() {
    Article article =
        new Article(
            "Multiple Tags Article",
            "Description",
            "Body",
            Arrays.asList("multi1", "multi2", "multi3"),
            user.getId());
    articleRepository.save(article);

    Article fetched = articleRepository.findById(article.getId()).orElse(null);

    Assertions.assertNotNull(fetched);
    Assertions.assertEquals(3, fetched.getTags().size());
    Assertions.assertTrue(fetched.getTags().contains(new Tag("multi1")));
    Assertions.assertTrue(fetched.getTags().contains(new Tag("multi2")));
    Assertions.assertTrue(fetched.getTags().contains(new Tag("multi3")));
  }

  @Test
  public void should_preserve_tags_after_article_update() {
    Article article =
        new Article("Original Title", "Description", "Body", Arrays.asList("preserved"), user.getId());
    articleRepository.save(article);

    article.update("Updated Title", "", "");
    articleRepository.save(article);

    Article fetched = articleRepository.findById(article.getId()).orElse(null);

    Assertions.assertNotNull(fetched);
    Assertions.assertTrue(fetched.getTags().contains(new Tag("preserved")));
  }

  @Test
  public void should_handle_tags_with_special_characters() {
    Article article =
        new Article(
            "Special Tags Article",
            "Description",
            "Body",
            Arrays.asList("c++", "c#", "node.js"),
            user.getId());
    articleRepository.save(article);

    List<String> allTags = tagReadService.all();

    Assertions.assertTrue(allTags.contains("c++"));
    Assertions.assertTrue(allTags.contains("c#"));
    Assertions.assertTrue(allTags.contains("node.js"));
  }

  @Test
  public void should_handle_tags_with_numbers() {
    Article article =
        new Article(
            "Numbered Tags Article",
            "Description",
            "Body",
            Arrays.asList("java11", "python3", "es2020"),
            user.getId());
    articleRepository.save(article);

    Tag java11Tag = articleMapper.findTag("java11");
    Tag python3Tag = articleMapper.findTag("python3");
    Tag es2020Tag = articleMapper.findTag("es2020");

    Assertions.assertNotNull(java11Tag);
    Assertions.assertNotNull(python3Tag);
    Assertions.assertNotNull(es2020Tag);
  }

  @Test
  public void should_return_empty_list_when_no_tags_exist() {
    List<String> allTags = tagReadService.all();

    Assertions.assertNotNull(allTags);
  }
}
