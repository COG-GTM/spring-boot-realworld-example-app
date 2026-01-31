package io.spring.graphql;

import static org.hamcrest.CoreMatchers.equalTo;
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
import io.spring.application.data.ProfileData;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.FollowRelation;
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

@SpringBootTest(classes = {DgsAutoConfiguration.class, RelationMutation.class})
@Import({TestSecurityConfig.class})
public class RelationMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

    @MockBean private ProfileQueryService profileQueryService;

    // MockBeans for other DgsComponents discovered by DGS auto-configuration
    @MockBean private ArticleQueryService articleQueryService;
    @MockBean private ArticleCommandService articleCommandService;
    @MockBean private ArticleRepository articleRepository;
    @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
    @MockBean private CommentQueryService commentQueryService;
    @MockBean private CommentRepository commentRepository;
    @MockBean private UserQueryService userQueryService;
    @MockBean private JwtService jwtService;
    @MockBean private TagsQueryService tagsQueryService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private UserService userService;

    private User user;
  private User targetUser;
  private ProfileData targetProfileData;

    @BeforeEach
    public void setUp() {
      user = new User("john@jacob.com", "johnjacob", "123", "", "");
      targetUser = new User("target@test.com", "targetuser", "123", "target bio", "target-image.jpg");
      targetProfileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), true);
      // Set up anonymous authentication to avoid NullPointerException in SecurityUtil.getCurrentUser()
      AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
          "anonymous", "anonymousUser", java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
      SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

  @Test
  public void should_follow_user_success() {
    setAuthentication(user);
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.of(targetProfileData));

    String mutation = "mutation { followUser(username: \"targetuser\") { profile { username following bio } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile.username");
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile.following");

    assertThat(username, equalTo("targetuser"));
    assertThat(following, equalTo(true));

    verify(userRepository, atLeastOnce()).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_fail_follow_user_without_authentication() {
    String mutation = "mutation { followUser(username: \"targetuser\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_follow_user_when_target_not_found() {
    setAuthentication(user);
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { followUser(username: \"nonexistent\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_unfollow_user_success() {
    setAuthentication(user);
    FollowRelation relation = new FollowRelation(user.getId(), targetUser.getId());
    ProfileData unfollowedProfile = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), false);
    
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId()))).thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.of(unfollowedProfile));

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username following } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile.username");
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile.following");

    assertThat(username, equalTo("targetuser"));
    assertThat(following, equalTo(false));

    verify(userRepository, atLeastOnce()).removeRelation(any(FollowRelation.class));
  }

  @Test
  public void should_fail_unfollow_user_without_authentication() {
    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_unfollow_user_when_target_not_found() {
    setAuthentication(user);
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { unfollowUser(username: \"nonexistent\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_unfollow_user_when_relation_not_found() {
    setAuthentication(user);
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId()))).thenReturn(Optional.empty());

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  private void setAuthentication(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
