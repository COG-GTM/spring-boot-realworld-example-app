package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.application.data.ArticleFavoriteCount;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleQueryServiceCursorTest {

  @Mock private ArticleReadService articleReadService;
  @Mock private UserRelationshipQueryService userRelationshipQueryService;
  @Mock private ArticleFavoritesReadService articleFavoritesReadService;

  private ArticleQueryService articleQueryService;
  private final User currentUser = new User("john@example.com", "john", "123", "", "");

  @BeforeEach
  public void setUp() {
    articleQueryService =
        new ArticleQueryService(
            articleReadService, userRelationshipQueryService, articleFavoritesReadService);
  }

  private ArticleData articleData(String id, String authorId) {
    return new ArticleData(
        id,
        "slug-" + id,
        "title",
        "desc",
        "body",
        false,
        0,
        new DateTime(1000L),
        new DateTime(1000L),
        Collections.emptyList(),
        new ProfileData(authorId, "author", "bio", "image", false));
  }

  @Test
  public void should_return_empty_when_article_is_not_found_by_id_or_slug() {
    when(articleReadService.findById("not-exist")).thenReturn(null);
    when(articleReadService.findBySlug("not-exist")).thenReturn(null);

    assertThat(articleQueryService.findById("not-exist", currentUser)).isEmpty();
    assertThat(articleQueryService.findBySlug("not-exist", currentUser)).isEmpty();
  }

  @Test
  public void should_not_fill_extra_info_for_anonymous_user() {
    ArticleData data = articleData("1", "authorId");
    when(articleReadService.findBySlug("slug-1")).thenReturn(data);

    Optional<ArticleData> found = articleQueryService.findBySlug("slug-1", null);

    assertThat(found).containsSame(data);
    assertThat(data.isFavorited()).isFalse();
    assertThat(data.getProfileData().isFollowing()).isFalse();
  }

  @Test
  public void should_fill_favorite_and_following_info_for_current_user() {
    ArticleData data = articleData("1", "authorId");
    when(articleReadService.findById("1")).thenReturn(data);
    when(articleFavoritesReadService.isUserFavorite(currentUser.getId(), "1")).thenReturn(true);
    when(articleFavoritesReadService.articleFavoriteCount("1")).thenReturn(3);
    when(userRelationshipQueryService.isUserFollowing(currentUser.getId(), "authorId"))
        .thenReturn(true);

    Optional<ArticleData> found = articleQueryService.findById("1", currentUser);

    assertThat(found).isPresent();
    assertThat(found.get().isFavorited()).isTrue();
    assertThat(found.get().getFavoritesCount()).isEqualTo(3);
    assertThat(found.get().getProfileData().isFollowing()).isTrue();
  }

  @Test
  public void should_return_empty_cursor_pager_when_no_article_matches() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    when(articleReadService.findArticlesWithCursor(null, null, null, page))
        .thenReturn(new ArrayList<>());

    CursorPager<ArticleData> pager =
        articleQueryService.findRecentArticlesWithCursor(null, null, null, page, currentUser);

    assertThat(pager.getData()).isEmpty();
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_drop_the_extra_article_and_flag_next_page() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    when(articleReadService.findArticlesWithCursor(null, null, null, page))
        .thenReturn(new ArrayList<>(Arrays.asList("1", "2", "3")));
    List<ArticleData> articles =
        new ArrayList<>(Arrays.asList(articleData("1", "authorId"), articleData("2", "authorId")));
    when(articleReadService.findArticles(Arrays.asList("1", "2"))).thenReturn(articles);
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(
            Arrays.asList(new ArticleFavoriteCount("1", 5), new ArticleFavoriteCount("2", 0)));
    when(articleFavoritesReadService.userFavorites(anyList(), eq(currentUser)))
        .thenReturn(new HashSet<>(Arrays.asList("2")));
    when(userRelationshipQueryService.followingAuthors(eq(currentUser.getId()), anyList()))
        .thenReturn(new HashSet<>(Arrays.asList("authorId")));

    CursorPager<ArticleData> pager =
        articleQueryService.findRecentArticlesWithCursor(null, null, null, page, currentUser);

    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.getData()).extracting(ArticleData::getId).containsExactly("1", "2");
    assertThat(pager.getData().get(0).getFavoritesCount()).isEqualTo(5);
    assertThat(pager.getData().get(0).isFavorited()).isFalse();
    assertThat(pager.getData().get(1).isFavorited()).isTrue();
    assertThat(pager.getData().get(0).getProfileData().isFollowing()).isTrue();
  }

  @Test
  public void should_reverse_articles_when_paging_backwards() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 5, Direction.PREV);
    when(articleReadService.findArticlesWithCursor(null, null, null, page))
        .thenReturn(new ArrayList<>(Arrays.asList("1", "2")));
    when(articleReadService.findArticles(Arrays.asList("2", "1")))
        .thenReturn(
            new ArrayList<>(
                Arrays.asList(articleData("2", "authorId"), articleData("1", "authorId"))));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(
            Arrays.asList(new ArticleFavoriteCount("1", 0), new ArticleFavoriteCount("2", 0)));

    CursorPager<ArticleData> pager =
        articleQueryService.findRecentArticlesWithCursor(null, null, null, page, null);

    assertThat(pager.getData()).extracting(ArticleData::getId).containsExactly("2", "1");
    assertThat(pager.hasPrevious()).isFalse();
  }

  @Test
  public void should_return_empty_feed_cursor_when_user_follows_nobody() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 5, Direction.NEXT);
    when(userRelationshipQueryService.followedUsers(currentUser.getId()))
        .thenReturn(new ArrayList<>());

    CursorPager<ArticleData> pager = articleQueryService.findUserFeedWithCursor(currentUser, page);

    assertThat(pager.getData()).isEmpty();
    assertThat(pager.hasNext()).isFalse();
  }

  @Test
  public void should_drop_the_extra_feed_article_and_flag_next_page() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 1, Direction.NEXT);
    List<String> followed = Arrays.asList("authorId");
    when(userRelationshipQueryService.followedUsers(currentUser.getId())).thenReturn(followed);
    when(articleReadService.findArticlesOfAuthorsWithCursor(followed, page))
        .thenReturn(
            new ArrayList<>(
                Arrays.asList(articleData("1", "authorId"), articleData("2", "authorId"))));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("1", 0)));
    when(articleFavoritesReadService.userFavorites(anyList(), eq(currentUser)))
        .thenReturn(new HashSet<>());
    when(userRelationshipQueryService.followingAuthors(eq(currentUser.getId()), anyList()))
        .thenReturn(new HashSet<>());

    CursorPager<ArticleData> pager = articleQueryService.findUserFeedWithCursor(currentUser, page);

    assertThat(pager.getData()).extracting(ArticleData::getId).containsExactly("1");
    assertThat(pager.hasNext()).isTrue();
  }

  @Test
  public void should_return_article_count_with_empty_list_when_nothing_matches() {
    Page page = new Page(0, 20);
    when(articleReadService.queryArticles(anyString(), any(), any(), eq(page)))
        .thenReturn(new ArrayList<>());
    when(articleReadService.countArticle(anyString(), any(), any())).thenReturn(0);

    ArticleDataList result =
        articleQueryService.findRecentArticles("tag", null, null, page, currentUser);

    assertThat(result.getArticleDatas()).isEmpty();
    assertThat(result.getCount()).isZero();
  }

  @Test
  public void should_return_empty_feed_when_user_follows_nobody() {
    Page page = new Page(0, 20);
    when(userRelationshipQueryService.followedUsers(currentUser.getId()))
        .thenReturn(new ArrayList<>());

    ArticleDataList result = articleQueryService.findUserFeed(currentUser, page);

    assertThat(result.getArticleDatas()).isEmpty();
    assertThat(result.getCount()).isZero();
  }

  @Test
  public void should_return_feed_articles_with_feed_size() {
    Page page = new Page(0, 20);
    List<String> followed = Arrays.asList("authorId");
    when(userRelationshipQueryService.followedUsers(currentUser.getId())).thenReturn(followed);
    when(articleReadService.findArticlesOfAuthors(followed, page))
        .thenReturn(new ArrayList<>(Arrays.asList(articleData("1", "authorId"))));
    when(articleReadService.countFeedSize(followed)).thenReturn(1);
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("1", 2)));
    when(articleFavoritesReadService.userFavorites(anyList(), eq(currentUser)))
        .thenReturn(new HashSet<>(Arrays.asList("1")));
    when(userRelationshipQueryService.followingAuthors(eq(currentUser.getId()), anyList()))
        .thenReturn(new HashSet<>());

    ArticleDataList result = articleQueryService.findUserFeed(currentUser, page);

    assertThat(result.getCount()).isEqualTo(1);
    assertThat(result.getArticleDatas()).hasSize(1);
    assertThat(result.getArticleDatas().get(0).isFavorited()).isTrue();
    assertThat(result.getArticleDatas().get(0).getFavoritesCount()).isEqualTo(2);
    assertThat(result.getArticleDatas().get(0).getProfileData().isFollowing()).isFalse();
  }
}
