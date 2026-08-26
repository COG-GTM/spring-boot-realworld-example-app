package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ProfileDatafetcher profileDatafetcher;
  private User currentUser;

  @BeforeEach
  public void setUp() {
    profileDatafetcher = new ProfileDatafetcher(profileQueryService);
    currentUser = new User("email@test.com", "username", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, java.util.List.of()));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_query_profile_of_local_context_user() {
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(currentUser);
    when(profileQueryService.findByUsername(eq("username"), eq(currentUser)))
        .thenReturn(Optional.of(new ProfileData("id", "username", "bio", "image", true)));

    Profile profile = profileDatafetcher.getUserProfile(dataFetchingEnvironment);

    assertThat(profile.getUsername(), is("username"));
    assertThat(profile.getBio(), is("bio"));
    assertThat(profile.getImage(), is("image"));
    assertThat(profile.getFollowing(), is(true));
  }

  @Test
  public void should_wrap_queried_profile_into_payload() {
    when(dataFetchingEnvironment.<String>getArgument("username")).thenReturn("other");
    when(profileQueryService.findByUsername(eq("other"), eq(currentUser)))
        .thenReturn(Optional.of(new ProfileData("id", "other", "bio", "image", false)));

    ProfilePayload payload = profileDatafetcher.queryProfile("other", dataFetchingEnvironment);

    assertThat(payload.getProfile().getUsername(), is("other"));
    assertThat(payload.getProfile().getFollowing(), is(false));
  }

  @Test
  public void should_throw_when_profile_is_not_found() {
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(currentUser);
    when(profileQueryService.findByUsername(eq("username"), eq(currentUser)))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> profileDatafetcher.getUserProfile(dataFetchingEnvironment));
  }
}
