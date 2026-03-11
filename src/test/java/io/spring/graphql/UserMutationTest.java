package io.spring.graphql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, UserMutation.class, MeDatafetcher.class})
@Import({TestSecurityConfig.class})
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private UserService userService;

  @MockBean private PasswordEncoder passwordEncoder;

    @MockBean private JwtService jwtService;

        @MockBean private UserQueryService userQueryService;

      // MockBeans for other DgsComponents discovered by DGS auto-configuration
      @MockBean private ArticleQueryService articleQueryService;
      @MockBean private ArticleCommandService articleCommandService;
      @MockBean private ArticleRepository articleRepository;
      @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
      @MockBean private CommentQueryService commentQueryService;
      @MockBean private CommentRepository commentRepository;
      @MockBean private ProfileQueryService profileQueryService;
      @MockBean private TagsQueryService tagsQueryService;

      private User user;
  private String defaultAvatar;

    @BeforeEach
    public void setUp() {
      defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
      user = new User("john@jacob.com", "johnjacob", "123", "", defaultAvatar);
      // Set up anonymous authentication to avoid NullPointerException in SecurityUtil.getCurrentUser()
      AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
          "anonymous", "anonymousUser", java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
      SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

  @Test
  public void should_create_user_success() {
    when(userService.createUser(any())).thenReturn(user);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String mutation =
        "mutation { createUser(input: {email: \"john@jacob.com\", username: \"johnjacob\", password: \"123\"}) { "
            + "... on UserPayload { user { email username token } } "
            + "... on Error { message } } }";

    String email = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.email");
    String username = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.username");
    String token = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user.token");

    assertThat(email, equalTo("john@jacob.com"));
    assertThat(username, equalTo("johnjacob"));
    assertThat(token, equalTo("test-token"));

    verify(userService, atLeastOnce()).createUser(any());
  }

  @Test
  public void should_login_success() {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("123"), eq("123"))).thenReturn(true);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String mutation = "mutation { login(email: \"john@jacob.com\", password: \"123\") { user { email username token } } }";

    String email = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.email");
    String username = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.username");
    String token = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user.token");

    assertThat(email, equalTo("john@jacob.com"));
    assertThat(username, equalTo("johnjacob"));
    assertThat(token, equalTo("test-token"));
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrong"), eq("123"))).thenReturn(false);

    String mutation = "mutation { login(email: \"john@jacob.com\", password: \"wrong\") { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_login_with_nonexistent_user() {
    when(userRepository.findByEmail(eq("nonexistent@test.com"))).thenReturn(Optional.empty());

    String mutation = "mutation { login(email: \"nonexistent@test.com\", password: \"123\") { user { email } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_update_user_success() {
    when(jwtService.toToken(any())).thenReturn("test-token");

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String mutation =
        "mutation { updateUser(changes: {email: \"newemail@test.com\", bio: \"new bio\"}) { user { email username token } } }";

    String email = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser.user.email");
    String username = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser.user.username");

    assertThat(email, equalTo("john@jacob.com"));
    assertThat(username, equalTo("johnjacob"));

    verify(userService, atLeastOnce()).updateUser(any());
  }

  @Test
  public void should_return_null_when_update_user_without_authentication() {
    String mutation =
        "mutation { updateUser(changes: {email: \"newemail@test.com\"}) { user { email } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateUser");

    assertThat(result, nullValue());
  }
}
