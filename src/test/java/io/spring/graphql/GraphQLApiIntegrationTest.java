package io.spring.graphql;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
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
public class GraphQLApiIntegrationTest {
  @Autowired private MockMvc mvc;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private UserRepository userRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
  }

  private ResultActions graphql(String query) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("query", query);
    return mvc.perform(
        post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
  }

  @Test
  public void should_accept_plain_graphql_request_body_without_authentication() throws Exception {
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));

    graphql("{ articles(first: 10) { edges { cursor } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.articles.edges", hasSize(0)));
  }

  @Test
  public void should_build_article_page_info_from_cursor_pager() throws Exception {
    ArticleData first = TestHelper.articleDataFixture("1", user);
    ArticleData second = TestHelper.articleDataFixture("2", user);
    second.setCreatedAt(first.getCreatedAt().minusHours(1));
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(first, second), Direction.NEXT, true));

    graphql(
            "{ articles(first: 2) { pageInfo { startCursor endCursor hasNextPage hasPreviousPage }"
                + " edges { cursor node { slug } } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(
            jsonPath("$.data.articles.pageInfo.startCursor", equalTo(first.getCursor().toString())))
        .andExpect(
            jsonPath("$.data.articles.pageInfo.endCursor", equalTo(second.getCursor().toString())))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasNextPage", equalTo(true)))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasPreviousPage", equalTo(false)))
        .andExpect(
            jsonPath(
                "$.data.articles.edges[*].node.slug", contains(first.getSlug(), second.getSlug())));
  }

  @Test
  public void should_return_null_cursors_for_empty_backward_article_page() throws Exception {
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.PREV, true));

    graphql(
            "{ articles(last: 2) { pageInfo { startCursor endCursor hasNextPage hasPreviousPage }"
                + " } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.articles.pageInfo.startCursor", nullValue()))
        .andExpect(jsonPath("$.data.articles.pageInfo.endCursor", nullValue()))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasNextPage", equalTo(false)))
        .andExpect(jsonPath("$.data.articles.pageInfo.hasPreviousPage", equalTo(true)));
  }

  @Test
  public void should_build_comment_page_info_from_cursor_pager() throws Exception {
    ArticleData article = TestHelper.articleDataFixture("1", user);
    when(articleQueryService.findBySlug(eq(article.getSlug()), any()))
        .thenReturn(Optional.of(article));
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData(user.getId(), user.getUsername(), "", "", false);
    CommentData newer = new CommentData("c1", "newer", article.getId(), now, now, profile);
    CommentData older =
        new CommentData("c2", "older", article.getId(), now.minusHours(1), now, profile);
    when(commentQueryService.findByArticleIdWithCursor(eq(article.getId()), any(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(newer, older), Direction.PREV, true));

    graphql(
            "{ article(slug: \""
                + article.getSlug()
                + "\") { comments(last: 2) { pageInfo { startCursor endCursor hasNextPage"
                + " hasPreviousPage } edges { node { id } } } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(
            jsonPath(
                "$.data.article.comments.pageInfo.startCursor",
                equalTo(newer.getCursor().toString())))
        .andExpect(
            jsonPath(
                "$.data.article.comments.pageInfo.endCursor",
                equalTo(older.getCursor().toString())))
        .andExpect(jsonPath("$.data.article.comments.pageInfo.hasNextPage", equalTo(false)))
        .andExpect(jsonPath("$.data.article.comments.pageInfo.hasPreviousPage", equalTo(true)))
        .andExpect(jsonPath("$.data.article.comments.edges[*].node.id", contains("c1", "c2")));
  }

  @Test
  public void should_return_unauthenticated_error_for_invalid_login() throws Exception {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.empty());

    graphql("mutation { login(email: \"john@jacob.com\", password: \"123\") { user { email } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].message", equalTo("invalid email or password")))
        .andExpect(jsonPath("$.errors[0].extensions.errorType", equalTo("UNAUTHENTICATED")))
        .andExpect(jsonPath("$.errors[0].path[0]", equalTo("login")));
  }

  @Test
  public void should_return_error_payload_for_password_longer_than_72_bytes() throws Exception {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

    graphql(
            "mutation { createUser(input: {email: \"john@jacob.com\", username: \"johnjacob\","
                + " password: \""
                + "\u00e9".repeat(37)
                + "\"}) { ... on Error { message errors { key value } } } }")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").doesNotExist())
        .andExpect(jsonPath("$.data.createUser.message", equalTo("BAD_REQUEST")))
        .andExpect(jsonPath("$.data.createUser.errors[0].key", equalTo("password")))
        .andExpect(
            jsonPath(
                "$.data.createUser.errors[0].value[0]",
                equalTo("is too long (maximum is 72 bytes)")));
    verify(userRepository, never()).save(any());
  }
}
