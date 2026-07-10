package io.spring.graphql;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Shared base for GraphQL (Netflix DGS) datafetcher/mutation tests. Boots the full Spring context
 * so the real GraphQL schema and wiring are exercised, while every collaborating service/repository
 * is replaced with a Mockito mock so behaviour can be controlled per test. All subclasses share the
 * exact same context configuration so Spring caches and reuses a single application context.
 */
@SpringBootTest
abstract class DgsGraphQLTestBase {

  @Autowired protected DgsQueryExecutor dgsQueryExecutor;

  @MockBean protected ArticleQueryService articleQueryService;
  @MockBean protected CommentQueryService commentQueryService;
  @MockBean protected ProfileQueryService profileQueryService;
  @MockBean protected TagsQueryService tagsQueryService;
  @MockBean protected UserQueryService userQueryService;
  @MockBean protected UserRepository userRepository;
  @MockBean protected ArticleRepository articleRepository;
  @MockBean protected CommentRepository commentRepository;
  @MockBean protected ArticleFavoriteRepository articleFavoriteRepository;
  @MockBean protected ArticleCommandService articleCommandService;
  @MockBean protected UserService userService;
  @MockBean protected JwtService jwtService;
  @MockBean protected PasswordEncoder passwordEncoder;

  protected User user;

  @BeforeEach
  void setUpCurrentUser() {
    user = new User("john@jacob.com", "johnjacob", "123", "bio", "avatar.png");
    setAnonymous();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  protected void setAuthenticatedUser(User currentUser) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  protected void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  protected ArticleData articleData(String seed, User author) {
    DateTime now = new DateTime();
    return new ArticleData(
        seed + "-id",
        seed,
        "title " + seed,
        "desc " + seed,
        "body " + seed,
        false,
        0,
        now,
        now,
        new ArrayList<>(Collections.singletonList("java")),
        profileData(author));
  }

  protected CommentData commentData(String id, User author) {
    DateTime now = new DateTime();
    return new CommentData(id, "comment body " + id, "article-id", now, now, profileData(author));
  }

  protected ProfileData profileData(User author) {
    return new ProfileData(
        author.getId(), author.getUsername(), author.getBio(), author.getImage(), false);
  }
}
