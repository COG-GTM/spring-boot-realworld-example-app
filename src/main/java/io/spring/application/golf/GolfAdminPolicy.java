package io.spring.application.golf;

import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Users listed in {@code golf.admin-usernames} (comma separated) may manage golf results. */
@Component
public class GolfAdminPolicy {
  private final Set<String> adminUsernames;

  public GolfAdminPolicy(@Value("${golf.admin-usernames:}") String adminUsernames) {
    this.adminUsernames =
        Arrays.stream(adminUsernames.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
  }

  public boolean isAdmin(User user) {
    return user != null && adminUsernames.contains(user.getUsername());
  }
}
