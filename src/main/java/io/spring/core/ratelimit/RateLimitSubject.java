package io.spring.core.ratelimit;

import io.spring.core.user.User;

public final class RateLimitSubject {
  private RateLimitSubject() {}

  public static String of(User user) {
    return "user:" + user.getId();
  }

  public static String ofAddress(String remoteAddress) {
    return "ip:" + remoteAddress;
  }
}
