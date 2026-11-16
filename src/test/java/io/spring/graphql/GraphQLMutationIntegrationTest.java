package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.data.UserData;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      ArticleMutation.class,
      ArticleDatafetcher.class,
      MeDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class
    })
public class GraphQLMutationIntegrationTest {
  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;
  @MockBean private PasswordEncoder passwordEncoder;
  @MockBean private UserService userService;
  @MockBean private UserQueryService userQueryService;
  @MockBean private JwtService jwtService;
  @MockBean private ArticleCommandService articleCommandService;
  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
  @MockBean private ArticleRepository articleRepository;
  @MockBean private ArticleQueryService articleQueryService;

  private User user;

  public static class Invalid {
    @NotBlank(message = "can't be empty")
    private final String title;

    public Invalid(String title) {
      this.title = title;
    }

    public String getTitle() {
      return title;
    }
  }

  public static class InvalidUser {
    @NotBlank(message = "can't be empty")
    private final String email = "";

    @NotBlank(message = "can't be empty")
    private final String username = "";

    @NotBlank(message = "can't be empty")
    private final String password = "";
  }

  @BeforeEach
  public void setUp() {
    user = new User("john@jacob.com", "johnjacob", "encoded", "", "");
    when(jwtService.toToken(any())).thenReturn("generated-token");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ---- UserMutation ----

  @Test
  public void create_user_should_return_user_payload_with_token() {
    anonymous();
    when(userService.createUser(any())).thenReturn(user);

    DocumentContext doc =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"john@jacob.com\", username: \"johnjacob\","
                + " password: \"secret\"}) { __typename ... on UserPayload { user { email"
                + " username token } } } }");

    assertEquals("UserPayload", doc.read("data.createUser.__typename"));
    assertEquals("john@jacob.com", doc.read("data.createUser.user.email"));
    assertEquals("johnjacob", doc.read("data.createUser.user.username"));
    assertEquals("generated-token", doc.read("data.createUser.user.token"));
    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertEquals("john@jacob.com", captor.getValue().getEmail());
    assertEquals("johnjacob", captor.getValue().getUsername());
    assertEquals("secret", captor.getValue().getPassword());
  }

  @Test
  public void create_user_should_return_error_union_on_validation_failure() {
    anonymous();
    Set<ConstraintViolation<InvalidUser>> violations =
        Validation.buildDefaultValidatorFactory().getValidator().validate(new InvalidUser());
    when(userService.createUser(any())).thenThrow(new ConstraintViolationException(violations));

    DocumentContext doc =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"\", username: \"\", password: \"\"}) {"
                + " __typename ... on Error { message errors { key value } } } }");

    assertEquals("Error", doc.read("data.createUser.__typename"));
    assertEquals("BAD_REQUEST", doc.read("data.createUser.message"));
    List<String> keys = doc.read("data.createUser.errors[*].key");
    assertTrue(keys.containsAll(Arrays.asList("email", "username", "password")));
    List<String> emailErrors = doc.read("data.createUser.errors[?(@.key == 'email')].value[0]");
    assertEquals(Collections.singletonList("can't be empty"), emailErrors);
  }

  @Test
  public void login_should_return_user_payload_for_valid_credentials() {
    anonymous();
    when(userRepository.findByEmail("john@jacob.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { login(email: \"john@jacob.com\", password: \"secret\") { user { token"
                + " } } }",
            "data.login.user.token");

    assertEquals("generated-token", token);
  }

  @Test
  public void login_should_return_unauthenticated_error_for_wrong_password() {
    anonymous();
    when(userRepository.findByEmail("john@jacob.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"john@jacob.com\", password: \"wrong\") { user { token }"
                + " } }");

    assertEquals(1, result.getErrors().size());
    assertEquals("invalid email or password", result.getErrors().get(0).getMessage());
    assertEquals("UNAUTHENTICATED", result.getErrors().get(0).getExtensions().get("errorType"));
    assertNull(((Map<String, Object>) result.getData()).get("login"));
  }

  @Test
  public void login_should_return_unauthenticated_error_for_unknown_email() {
    anonymous();
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"ghost@example.com\", password: \"x\") { user { token } }"
                + " }");

    assertEquals("UNAUTHENTICATED", result.getErrors().get(0).getExtensions().get("errorType"));
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  public void update_user_should_apply_changes_for_current_user() {
    authenticate(user);

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateUser(changes: {bio: \"new bio\", username: \"johnjacob\"}) {"
                + " user { username } } }",
            "data.updateUser.user.username");

    assertEquals("johnjacob", username);
    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertEquals(user, captor.getValue().getTargetUser());
    assertEquals("new bio", captor.getValue().getParam().getBio());
  }

  @Test
  public void update_user_should_return_null_for_anonymous_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateUser(changes: {bio: \"new bio\"}) { user { username } } }");

    assertTrue(result.getErrors().isEmpty());
    assertNull(((Map<String, Object>) result.getData()).get("updateUser"));
    verify(userService, never()).updateUser(any());
  }

  // ---- MeDatafetcher ----

  @Test
  public void me_should_return_current_user_with_request_token() {
    authenticate(user);
    when(userQueryService.findById(user.getId()))
        .thenReturn(Optional.of(new UserData(user.getId(), user.getEmail(), "johnjacob", "", "")));
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token request-token");

    DocumentContext doc =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ me { email username token } }", Collections.emptyMap(), headers);

    assertEquals("john@jacob.com", doc.read("data.me.email"));
    assertEquals("johnjacob", doc.read("data.me.username"));
    assertEquals("request-token", doc.read("data.me.token"));
  }

  @Test
  public void me_should_return_null_for_anonymous_user() {
    anonymous();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token ignored");

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ me { email } }", Collections.emptyMap(), Collections.emptyMap(), headers);

    assertTrue(result.getErrors().isEmpty());
    assertNull(((Map<String, Object>) result.getData()).get("me"));
  }

  // ---- ArticleMutation ----

  @Test
  public void create_article_should_create_and_resolve_article() {
    authenticate(user);
    Article article =
        new Article("New Article", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(article.getId(), user))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));

    DocumentContext doc =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createArticle(input: {title: \"New Article\", description: \"desc\","
                + " body: \"body\", tagList: [\"java\"]}) { article { slug title } } }");

    assertEquals("new-article", doc.read("data.createArticle.article.slug"));
    assertEquals("New Article", doc.read("data.createArticle.article.title"));
    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertEquals("New Article", captor.getValue().getTitle());
    assertEquals(Arrays.asList("java"), captor.getValue().getTagList());
  }

  @Test
  public void create_article_should_default_missing_tag_list_to_empty() {
    authenticate(user);
    Article article = new Article("No Tags", "desc", "body", Collections.emptyList(), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(article.getId(), user))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));

    dgsQueryExecutor.execute(
        "mutation { createArticle(input: {title: \"No Tags\", description: \"desc\", body:"
            + " \"body\"}) { article { slug } } }");

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertEquals(Collections.emptyList(), captor.getValue().getTagList());
  }

  @Test
  public void create_article_should_fail_for_anonymous_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"}) {"
                + " article { slug } } }");

    assertFalse(result.getErrors().isEmpty());
    assertTrue(
        result.getErrors().get(0).getMessage().contains("AuthenticationException"),
        result.getErrors().get(0).getMessage());
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void create_article_validation_failure_should_return_bad_request_error() {
    authenticate(user);
    Set<ConstraintViolation<Invalid>> violations =
        Validation.buildDefaultValidatorFactory().getValidator().validate(new Invalid(""));
    when(articleCommandService.createArticle(any(), eq(user)))
        .thenThrow(new ConstraintViolationException(violations));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"\", description: \"d\", body: \"b\"}) {"
                + " article { slug } } }");

    assertEquals(1, result.getErrors().size());
    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertEquals("BAD_REQUEST", extensions.get("errorType"));
    assertEquals(Collections.singletonList("can't be empty"), extensions.get("title"));
  }

  @Test
  public void update_article_should_succeed_for_author() {
    authenticate(user);
    Article article = new Article("Old Title", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);
    when(articleQueryService.findById(article.getId(), user))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateArticle(slug: \"old-title\", changes: {body: \"new body\"}) {"
                + " article { slug } } }",
            "data.updateArticle.article.slug");

    assertEquals("old-title", slug);
    verify(articleCommandService).updateArticle(eq(article), any());
  }

  @Test
  public void update_article_should_be_rejected_for_non_author() {
    User other = new User("other@example.com", "other", "123", "", "");
    authenticate(other);
    Article article = new Article("Old Title", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \"old-title\", changes: {body: \"hacked\"}) {"
                + " article { slug } } }");

    assertFalse(result.getErrors().isEmpty());
    assertTrue(result.getErrors().get(0).getMessage().contains("NoAuthorizationException"));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void update_article_should_fail_for_missing_article() {
    authenticate(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \"missing\", changes: {body: \"b\"}) { article {"
                + " slug } } }");

    assertFalse(result.getErrors().isEmpty());
    assertTrue(result.getErrors().get(0).getMessage().contains("ResourceNotFoundException"));
  }

  @Test
  public void favorite_article_should_save_favorite() {
    authenticate(user);
    Article article = new Article("Fav", "desc", "body", Arrays.asList("java"), "author-id");
    when(articleRepository.findBySlug("fav")).thenReturn(Optional.of(article));
    when(articleQueryService.findById(article.getId(), user))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { favoriteArticle(slug: \"fav\") { article { slug } } }",
            "data.favoriteArticle.article.slug");

    assertEquals("fav", slug);
    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertEquals(article.getId(), captor.getValue().getArticleId());
    assertEquals(user.getId(), captor.getValue().getUserId());
  }

  @Test
  public void favorite_article_should_fail_for_anonymous_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { favoriteArticle(slug: \"fav\") { article { slug } } }");

    assertFalse(result.getErrors().isEmpty());
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void unfavorite_article_should_remove_existing_favorite() {
    authenticate(user);
    Article article = new Article("Fav", "desc", "body", Arrays.asList("java"), "author-id");
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug("fav")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(article.getId(), user))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));

    dgsQueryExecutor.execute("mutation { unfavoriteArticle(slug: \"fav\") { article { slug } } }");

    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  public void unfavorite_article_should_be_noop_when_not_favorited() {
    authenticate(user);
    Article article = new Article("Fav", "desc", "body", Arrays.asList("java"), "author-id");
    when(articleRepository.findBySlug("fav")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());
    when(articleQueryService.findById(article.getId(), user))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfavoriteArticle(slug: \"fav\") { article { slug } } }");

    assertTrue(result.getErrors().isEmpty());
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void delete_article_should_succeed_for_author() {
    authenticate(user);
    Article article = new Article("Mine", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug("mine")).thenReturn(Optional.of(article));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { deleteArticle(slug: \"mine\") { success } }", "data.deleteArticle.success");

    assertTrue(success);
    verify(articleRepository).remove(article);
  }

  @Test
  public void delete_article_should_be_rejected_for_non_author() {
    User other = new User("other@example.com", "other", "123", "", "");
    authenticate(other);
    Article article = new Article("Mine", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug("mine")).thenReturn(Optional.of(article));

    ExecutionResult result =
        dgsQueryExecutor.execute("mutation { deleteArticle(slug: \"mine\") { success } }");

    assertFalse(result.getErrors().isEmpty());
    verify(articleRepository, never()).remove(any());
  }

  private static void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private static void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
