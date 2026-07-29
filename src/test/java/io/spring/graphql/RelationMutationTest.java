package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, RelationMutation.class})
public class RelationMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private User currentUser;
  private User target;

  @BeforeEach
  public void setUp() {
    currentUser = new User("current@test.com", "current", "123", "", "");
    target = new User("target@test.com", "target", "123", "target bio", "target image");
    authenticate(currentUser);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_follow_a_user() {
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    mockProfile(true);

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { followUser(username: \"%s\") { profile { username bio image following }"
                    + " } }",
                target.getUsername()));

    assertThat(result.<String>read("data.followUser.profile.username"))
        .isEqualTo(target.getUsername());
    assertThat(result.<String>read("data.followUser.profile.bio")).isEqualTo(target.getBio());
    assertThat(result.<Boolean>read("data.followUser.profile.following")).isTrue();
    verify(userRepository)
        .saveRelation(eq(new FollowRelation(currentUser.getId(), target.getId())));
  }

  @Test
  public void should_report_error_when_following_an_unknown_user() {
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_reject_following_without_a_current_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"target\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_unfollow_a_user() {
    FollowRelation relation = new FollowRelation(currentUser.getId(), target.getId());
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    mockProfile(false);

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { unfollowUser(username: \"%s\") { profile { username following } } }",
                target.getUsername()));

    assertThat(result.<String>read("data.unfollowUser.profile.username"))
        .isEqualTo(target.getUsername());
    assertThat(result.<Boolean>read("data.unfollowUser.profile.following")).isFalse();
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  public void should_report_error_when_unfollowing_an_unknown_user() {
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_report_error_when_there_is_no_relation_to_remove() {
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { unfollowUser(username: \"%s\") { profile { username } } }",
                target.getUsername()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_reject_unfollowing_without_a_current_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"target\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
    verify(userRepository, never()).removeRelation(any());
  }

  private void mockProfile(boolean following) {
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(currentUser)))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    target.getId(),
                    target.getUsername(),
                    target.getBio(),
                    target.getImage(),
                    following)));
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
