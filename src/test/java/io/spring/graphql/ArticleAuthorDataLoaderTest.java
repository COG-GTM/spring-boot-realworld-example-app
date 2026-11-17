package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class,
      ProfileDataLoader.class
    })
public class ArticleAuthorDataLoaderTest {
  private static final String QUERY =
      "{ articles(first: 10) { edges { node { slug author { username bio following } } } } }";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private UserRepository userRepository;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ArticleData article(String slug, String author) {
    DateTime now = new DateTime();
    return new ArticleData(
        slug,
        slug,
        slug,
        "desc",
        "body",
        false,
        0,
        now,
        now,
        Collections.emptyList(),
        new ProfileData(author + "-id", author, "", "", false));
  }

  private void givenArticles(ArticleData... articles) {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(new CursorPager<>(Arrays.asList(articles), Direction.NEXT, false));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_load_all_article_authors_in_one_batch() {
    givenArticles(article("a1", "alice"), article("a2", "bob"), article("a3", "alice"));
    Map<String, ProfileData> profiles = new HashMap<>();
    profiles.put("alice", new ProfileData("alice-id", "alice", "alice bio", "", false));
    profiles.put("bob", new ProfileData("bob-id", "bob", "bob bio", "", true));
    when(profileQueryService.findByUsernames(anyCollection(), isNull())).thenReturn(profiles);

    ExecutionResult result = dgsQueryExecutor.execute(QUERY);

    Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(profileQueryService, times(1)).findByUsernames(captor.capture(), isNull());
    Assertions.assertEquals(
        new HashSet<>(Arrays.asList("alice", "bob")), new HashSet<>(captor.getValue()));
    verify(profileQueryService, never()).findByUsername(any(), any());

    List<Map<String, Object>> edges =
        (List<Map<String, Object>>)
            ((Map<String, Object>) ((Map<String, Object>) result.getData()).get("articles"))
                .get("edges");
    Assertions.assertEquals(3, edges.size());
    Map<String, Object> secondAuthor =
        (Map<String, Object>) ((Map<String, Object>) edges.get(1).get("node")).get("author");
    Assertions.assertEquals("bob", secondAuthor.get("username"));
    Assertions.assertEquals("bob bio", secondAuthor.get("bio"));
    Assertions.assertEquals(true, secondAuthor.get("following"));
    Map<String, Object> thirdAuthor =
        (Map<String, Object>) ((Map<String, Object>) edges.get(2).get("node")).get("author");
    Assertions.assertEquals("alice", thirdAuthor.get("username"));
  }

  @Test
  public void should_report_error_when_author_profile_is_missing() {
    givenArticles(article("a1", "ghost"));
    when(profileQueryService.findByUsernames(anyCollection(), isNull()))
        .thenReturn(Collections.emptyMap());

    ExecutionResult result = dgsQueryExecutor.execute(QUERY);

    Assertions.assertFalse(result.getErrors().isEmpty());
    Assertions.assertTrue(
        result.getErrors().stream()
            .anyMatch(e -> e.getPath() != null && e.getPath().contains("author")),
        result.getErrors().toString());
  }
}
