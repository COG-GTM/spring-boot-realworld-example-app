package io.spring.application.golf;

import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Users whose ids are listed in {@code golf.admin-user-ids} (comma separated) may manage golf
 * results.
 */
@Component
public class GolfAdminPolicy {
  private final Set<String> adminUserIds;

  public GolfAdminPolicy(@Value("${golf.admin-user-ids:}") String adminUserIds) {
    this.adminUserIds =
        Arrays.stream(adminUserIds.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
  }

  public boolean isAdmin(User user) {
    return user != null && adminUserIds.contains(user.getId());
  }
}
