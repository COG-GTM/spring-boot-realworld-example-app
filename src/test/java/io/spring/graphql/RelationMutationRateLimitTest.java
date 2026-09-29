package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.ProfileQueryService;
import io.spring.core.ratelimit.RateLimitAction;
import io.spring.core.ratelimit.RateLimitDecision;
import io.spring.core.ratelimit.RateLimitExceededException;
import io.spring.core.ratelimit.RateLimitSubject;
import io.spring.core.ratelimit.RateLimiter;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class RelationMutationRateLimitTest {
  private UserRepository userRepository;
  private RateLimiter rateLimiter;
  private RelationMutation mutation;
  private User user;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    rateLimiter = mock(RateLimiter.class);
    mutation = new RelationMutation(userRepository, mock(ProfileQueryService.class), rateLimiter);
    user = new User("a@b.com", "alice", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_reject_follow_before_touching_repository_when_limited() {
    when(rateLimiter.acquireOrThrow(eq(RateLimitAction.FOLLOW), eq(RateLimitSubject.of(user))))
        .thenThrow(
            new RateLimitExceededException(
                RateLimitAction.FOLLOW, new RateLimitDecision(false, 1, 0, 5)));

    assertThrows(RateLimitExceededException.class, () -> mutation.follow("bob"));

    verify(userRepository, never()).findByUsername(any());
    verify(userRepository, never()).saveRelation(any());
  }
}
