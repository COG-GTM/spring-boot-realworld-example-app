package io.spring.graphql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ProfileData;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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

@SpringBootTest(classes = {DgsAutoConfiguration.class, ProfileDatafetcher.class})
@Import({TestSecurityConfig.class})
public class ProfileDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

    @MockBean private ProfileQueryService profileQueryService;

    // MockBeans for other DgsComponents discovered by DGS auto-configuration
    @MockBean private ArticleQueryService articleQueryService;
    @MockBean private ArticleCommandService articleCommandService;
    @MockBean private ArticleRepository articleRepository;
    @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
    @MockBean private CommentQueryService commentQueryService;
    @MockBean private CommentRepository commentRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private UserQueryService userQueryService;
    @MockBean private JwtService jwtService;
    @MockBean private TagsQueryService tagsQueryService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private UserService userService;

    private User user;
  private User targetUser;
  private ProfileData profileData;

    @BeforeEach
    public void setUp() {
      user = new User("john@jacob.com", "johnjacob", "123", "bio", "image.jpg");
      targetUser = new User("target@test.com", "targetuser", "123", "target bio", "target-image.jpg");
      profileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), false);
      // Set up anonymous authentication to avoid NullPointerException in SecurityUtil.getCurrentUser()
      AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
          "anonymous", "anonymousUser", java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
      SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

  @Test
  public void should_get_profile_by_username() {
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.of(profileData));

    String query = "query { profile(username: \"targetuser\") { profile { username bio image following } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    String bio = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.bio");
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertThat(username, equalTo("targetuser"));
    assertThat(bio, equalTo("target bio"));
    assertThat(following, equalTo(false));
  }

  @Test
  public void should_get_profile_with_following_status_when_authenticated() {
    setAuthentication(user);
    ProfileData followingProfile = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), true);
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.of(followingProfile));

    String query = "query { profile(username: \"targetuser\") { profile { username following } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertThat(username, equalTo("targetuser"));
    assertThat(following, equalTo(true));
  }

  @Test
  public void should_fail_get_profile_when_user_not_found() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any())).thenReturn(Optional.empty());

    String query = "query { profile(username: \"nonexistent\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_get_profile_with_image() {
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.of(profileData));

    String query = "query { profile(username: \"targetuser\") { profile { username image } } }";

    String image = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.image");

    assertThat(image, equalTo("target-image.jpg"));
  }

  @Test
  public void should_get_profile_without_authentication() {
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.of(profileData));

    String query = "query { profile(username: \"targetuser\") { profile { username bio } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    String bio = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.bio");

    assertThat(username, equalTo("targetuser"));
    assertThat(bio, equalTo("target bio"));
  }

  private void setAuthentication(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
