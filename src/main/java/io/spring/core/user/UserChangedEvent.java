package io.spring.core.user;

import lombok.Value;

@Value
public class UserChangedEvent {
  String userId;
}
