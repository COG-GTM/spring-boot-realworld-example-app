package io.spring.graphql;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.UserData;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Shared harness for the GraphQL (DGS) layer tests. Boots the full application context (using the
 * in-memory SQLite {@code test} profile) so that the real {@code @DgsComponent} fetchers, the DGS
 * schema and the {@link DgsQueryExecutor} are wired up, while every underlying service/repository is
 * replaced with a Mockito mock exactly like the REST {@code @WebMvcTest} tests do. Authentication is
 * driven directly through the {@link SecurityContextHolder} because {@link SecurityUtil} reads the
 * current user from it (no HTTP filter runs during in-process query execution).
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class GraphQLTestBase {

  @Autowired protected DgsQueryExecutor dgsQueryExecutor;

  @MockBean protected ArticleQueryService articleQueryService;
  @MockBean protected CommentQueryService commentQueryService;
  @MockBean protected ProfileQueryService profileQueryService;
  @MockBean protected TagsQueryService tagsQueryService;
  @MockBean protected UserQueryService userQueryService;
  @MockBean protected ArticleCommandService articleCommandService;
  @MockBean protected UserService userService;
  @MockBean protected ArticleRepository articleRepository;
  @MockBean protected CommentRepository commentRepository;
  @MockBean protected ArticleFavoriteRepository articleFavoriteRepository;
  @MockBean protected UserRepository userRepository;
  @MockBean protected UserReadService userReadService;
  @MockBean protected JwtService jwtService;
  @MockBean protected PasswordEncoder passwordEncoder;

  protected User user;
  protected UserData userData;
  protected String email;
  protected String username;
  protected String defaultAvatar;
  protected String token;

  protected void userFixture() {
    email = "john@jacob.com";
    username = "johnjacob";
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";

    user = new User(email, username, "123", "", defaultAvatar);
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.of(user));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    token = "token";
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(jwtService.toToken(eq(user))).thenReturn(token);
  }

  @BeforeEach
  public void setUpBase() {
    userFixture();
    loginAs(user);
  }

  @AfterEach
  public void tearDownBase() {
    SecurityContextHolder.clearContext();
  }

  /** Simulate an authenticated request by placing the given user in the security context. */
  protected void loginAs(User currentUser) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            currentUser, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  /** Simulate an anonymous request, mirroring what the security filter chain installs. */
  protected void logout() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
