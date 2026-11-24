package io.spring.graphql;

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
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Collections;
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

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      ArticleMutation.class,
      CommentDatafetcher.class,
      CommentMutation.class,
      MeDatafetcher.class,
      ProfileDatafetcher.class,
      RelationMutation.class,
      TagDatafetcher.class,
      UserMutation.class,
      GraphQLCustomizeExceptionHandler.class
    })
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
  @MockBean protected JwtService jwtService;
  @MockBean protected PasswordEncoder passwordEncoder;

  protected User user;

  @BeforeEach
  public void setUpUser() {
    user =
        new User(
            "john@jacob.com",
            "johnjacob",
            "123",
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    anonymous();
  }

  @AfterEach
  public void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  protected void loginAs(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  protected void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
