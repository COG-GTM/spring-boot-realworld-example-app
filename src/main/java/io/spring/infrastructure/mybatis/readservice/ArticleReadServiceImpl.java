package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.CursorPageParameter;
import io.spring.application.Page;
import io.spring.application.data.ArticleData;
import io.spring.core.article.ArticleReadService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ArticleReadServiceImpl implements ArticleReadService {
  private final MyBatisArticleReadServiceMapper mapper;

  public ArticleReadServiceImpl(MyBatisArticleReadServiceMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public ArticleData findById(String id) {
    return mapper.findById(id);
  }

  @Override
  public ArticleData findBySlug(String slug) {
    return mapper.findBySlug(slug);
  }

  @Override
  public List<String> queryArticles(String tag, String author, String favoritedBy, Page page) {
    return mapper.queryArticles(tag, author, favoritedBy, page);
  }

  @Override
  public int countArticle(String tag, String author, String favoritedBy) {
    return mapper.countArticle(tag, author, favoritedBy);
  }

  @Override
  public List<ArticleData> findArticles(List<String> articleIds) {
    return mapper.findArticles(articleIds);
  }

  @Override
  public List<ArticleData> findArticlesOfAuthors(List<String> authors, Page page) {
    return mapper.findArticlesOfAuthors(authors, page);
  }

  @Override
  public List<ArticleData> findArticlesOfAuthorsWithCursor(
      List<String> authors, CursorPageParameter page) {
    return mapper.findArticlesOfAuthorsWithCursor(authors, page);
  }

  @Override
  public int countFeedSize(List<String> authors) {
    return mapper.countFeedSize(authors);
  }

  @Override
  public List<String> findArticlesWithCursor(
      String tag, String author, String favoritedBy, CursorPageParameter page) {
    return mapper.findArticlesWithCursor(tag, author, favoritedBy, page);
  }
}
