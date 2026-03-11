package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class, ProfileDatafetcher.class})
public class ArticleDatafetcherTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ArticleQueryService articleQueryService;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private ProfileQueryService profileQueryService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "", "");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(testUser, null, null)
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldFindArticleBySlug() {
    String slug = "test-article";
    DateTime now = DateTime.now();
    ProfileData profileData = new ProfileData("user-id", "testuser", "bio", "image", false);
    ArticleData articleData = new ArticleData(
        "article-id",
        slug,
        "Test Article",
        "Test Description",
        "Test Body",
        false,
        0,
        now,
        now,
        Arrays.asList("java", "spring"),
        profileData
    );

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.of(articleData));
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(
        "{ article(slug: \"" + slug + "\") { slug title description body tagList } }",
        "data.article"
    );

    assertThat(result.get("slug")).isEqualTo(slug);
    assertThat(result.get("title")).isEqualTo("Test Article");
    assertThat(result.get("description")).isEqualTo("Test Description");
    assertThat(result.get("body")).isEqualTo("Test Body");
  }

  @Test
  void shouldReturnArticlesWithPagination() {
    DateTime now = DateTime.now();
    ProfileData profileData = new ProfileData("user-id", "testuser", "bio", "image", false);
    ArticleData articleData1 = new ArticleData(
        "article-id-1",
        "test-article-1",
        "Test Article 1",
        "Description 1",
        "Body 1",
        false,
        0,
        now,
        now,
        Arrays.asList("java"),
        profileData
    );
    ArticleData articleData2 = new ArticleData(
        "article-id-2",
        "test-article-2",
        "Test Article 2",
        "Description 2",
        "Body 2",
        false,
        0,
        now,
        now,
        Arrays.asList("spring"),
        profileData
    );

    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData1, articleData2),
        Direction.NEXT,
        false
    );

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(
        "{ articles(first: 10) { edges { node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }",
        "data.articles"
    );

    assertThat(result).isNotNull();
  }
}
