package io.spring.application.golf;

import io.spring.core.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GolfAdminPolicyTest {
  private static User user(String username) {
    return new User(username + "@example.com", username, "pw", "", "");
  }

  @Test
  public void should_allow_only_configured_user_ids() {
    User admin = user("golfadmin");
    User other = user("other");
    GolfAdminPolicy policy = new GolfAdminPolicy(" " + admin.getId() + " , " + other.getId() + " ");
    Assertions.assertTrue(policy.isAdmin(admin));
    Assertions.assertTrue(policy.isAdmin(other));
    Assertions.assertFalse(policy.isAdmin(user("golfadmin")));
    Assertions.assertFalse(policy.isAdmin(null));
  }

  @Test
  public void should_allow_nobody_when_unconfigured() {
    Assertions.assertFalse(new GolfAdminPolicy("").isAdmin(user("golfadmin")));
  }
}
