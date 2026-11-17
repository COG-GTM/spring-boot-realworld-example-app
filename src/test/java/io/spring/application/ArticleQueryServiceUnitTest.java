package io.spring.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.ArrayList;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleQueryServiceUnitTest {
  private ArticleReadService articleReadService;
  private UserRelationshipQueryService userRelationshipQueryService;
  private ArticleFavoritesReadService articleFavoritesReadService;
  private ArticleQueryService queryService;
  private User user;

  @BeforeEach
  public void setUp() {
    articleReadService = mock(ArticleReadService.class);
    userRelationshipQueryService = mock(UserRelationshipQueryService.class);
    articleFavoritesReadService = mock(ArticleFavoritesReadService.class);
    queryService =
        new ArticleQueryService(
            articleReadService, userRelationshipQueryService, articleFavoritesReadService);
    user = new User("follower@email.com", "follower", "123", "", "");
    when(userRelationshipQueryService.followedUsers(user.getId()))
        .thenReturn(Collections.singletonList("author-id"));
  }

  @Test
  public void should_not_query_extra_info_for_empty_user_feed_page() {
    Page page = new Page(10, 10);
    when(articleReadService.findArticlesOfAuthors(anyList(), eq(page)))
        .thenReturn(new ArrayList<>());
    when(articleReadService.countFeedSize(anyList())).thenReturn(1);

    ArticleDataList result = queryService.findUserFeed(user, page);

    Assertions.assertTrue(result.getArticleDatas().isEmpty());
    Assertions.assertEquals(1, result.getCount());
    verifyNoExtraInfoQueried();
  }

  @Test
  public void should_not_query_extra_info_for_empty_user_feed_cursor_page() {
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(new DateTime(), 10, Direction.NEXT);
    when(articleReadService.findArticlesOfAuthorsWithCursor(anyList(), eq(page)))
        .thenReturn(new ArrayList<ArticleData>());

    CursorPager<ArticleData> result = queryService.findUserFeedWithCursor(user, page);

    Assertions.assertTrue(result.getData().isEmpty());
    Assertions.assertFalse(result.hasNext());
    verifyNoExtraInfoQueried();
  }

  private void verifyNoExtraInfoQueried() {
    verify(articleFavoritesReadService, never()).articlesFavoriteCount(anyList());
    verify(articleFavoritesReadService, never()).userFavorites(anyList(), any(User.class));
    verify(userRelationshipQueryService, never()).followingAuthors(anyString(), anyList());
  }
}
