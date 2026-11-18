package io.spring.graphql;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.TestHelper;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.TagsQueryService;
import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
public class GraphQlEndpointTest {
  @Autowired private MockMvc mvc;

  @MockBean private TagsQueryService tagsQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private UserRepository userRepository;

  private final User author = new User("a@b.com", "author", "123", "", "");

  private ResultActions graphql(String query) throws Exception {
    String body = "{\"query\": \"" + query.replace("\"", "\\\"") + "\"}";
    return mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private ArticleData articleAt(String seed, long millis) {
    ArticleData data = TestHelper.articleDataFixture(seed, author);
    data.setCreatedAt(new DateTime(millis));
    data.setUpdatedAt(new DateTime(millis));
    return data;
  }

  @Test
  public void should_accept_plain_graphql_request_body_without_root_unwrapping() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    graphql("{ tags }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.tags[0]").value("java"))
        .andExpect(jsonPath("$.data.tags[1]").value("spring"));
  }

  @Test
  public void should_build_article_page_info_from_cursor_pager() throws Exception {
    ArticleData first = articleAt("1", 2000L);
    ArticleData second = articleAt("2", 1000L);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(first, second), Direction.NEXT, true));

    graphql(
            "{ articles(first: 2) { pageInfo { startCursor endCursor hasNextPage hasPreviousPage }"
                + " edges { cursor node { slug } } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.articles.pageInfo.startCursor").value("2000"))
        .andExpect(jsonPath("$.data.articles.pageInfo.endCursor").value("1000"))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasNextPage").value(true))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasPreviousPage").value(false))
        .andExpect(jsonPath("$.data.articles.edges[0].cursor").value("2000"))
        .andExpect(jsonPath("$.data.articles.edges[1].node.slug").value(second.getSlug()));
  }

  @Test
  public void should_return_null_cursors_for_empty_article_page() throws Exception {
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.PREV, false));

    graphql(
            "{ articles(last: 5) { pageInfo { startCursor endCursor hasNextPage hasPreviousPage }"
                + " edges { cursor } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.articles.pageInfo.startCursor", nullValue()))
        .andExpect(jsonPath("$.data.articles.pageInfo.endCursor", nullValue()))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasNextPage").value(false))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasPreviousPage").value(false))
        .andExpect(jsonPath("$.data.articles.edges").isEmpty());
  }

  @Test
  public void should_report_previous_page_when_paging_backwards() throws Exception {
    ArticleData article = articleAt("1", 3000L);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(article), Direction.PREV, true));

    graphql("{ articles(last: 1, before: \"4000\") { pageInfo { hasNextPage hasPreviousPage } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.articles.pageInfo.hasNextPage").value(false))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasPreviousPage").value(true));
  }

  @Test
  public void should_build_comment_page_info_from_cursor_pager() throws Exception {
    ArticleData article = articleAt("1", 1000L);
    when(articleQueryService.findBySlug(eq(article.getSlug()), any()))
        .thenReturn(Optional.of(article));
    ProfileData profile = new ProfileData(author.getId(), author.getUsername(), "", "", false);
    CommentData newer =
        new CommentData(
            "c2", "newer", article.getId(), new DateTime(5000L), new DateTime(5000L), profile);
    CommentData older =
        new CommentData(
            "c1", "older", article.getId(), new DateTime(4000L), new DateTime(4000L), profile);
    when(commentQueryService.findByArticleIdWithCursor(eq(article.getId()), any(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(newer, older), Direction.NEXT, false));

    graphql(
            "{ article(slug: \""
                + article.getSlug()
                + "\") { comments(first: 2) { pageInfo { startCursor endCursor hasNextPage"
                + " hasPreviousPage } edges { cursor node { id body } } } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.article.comments.pageInfo.startCursor").value("5000"))
        .andExpect(jsonPath("$.data.article.comments.pageInfo.endCursor").value("4000"))
        .andExpect(jsonPath("$.data.article.comments.pageInfo.hasNextPage").value(false))
        .andExpect(jsonPath("$.data.article.comments.pageInfo.hasPreviousPage").value(false))
        .andExpect(jsonPath("$.data.article.comments.edges[0].node.id").value("c2"))
        .andExpect(jsonPath("$.data.article.comments.edges[1].node.body").value("older"));
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() throws Exception {
    when(userRepository.findByEmail(eq("nobody@example.com"))).thenReturn(Optional.empty());

    graphql(
            "mutation { login(email: \"nobody@example.com\", password: \"wrong\")"
                + " { user { token } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.login", nullValue()))
        .andExpect(jsonPath("$.errors[0].message").value("invalid email or password"))
        .andExpect(jsonPath("$.errors[0].path[0]").value("login"))
        .andExpect(jsonPath("$.errors[0].extensions.errorType").value("UNAUTHENTICATED"));
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() throws Exception {
    graphql("{ articles { edges { cursor } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.articles", nullValue()))
        .andExpect(jsonPath("$.errors[0].path[0]").value("articles"))
        .andExpect(jsonPath("$.errors[0].message", containsString("IllegalArgumentException")));
  }
}
