package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class RegisterParamTest {

  @Test
  public void should_create_with_all_args() {
    RegisterParam param = new RegisterParam("test@test.com", "testuser", "password");

    assertEquals("test@test.com", param.getEmail());
    assertEquals("testuser", param.getUsername());
    assertEquals("password", param.getPassword());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    RegisterParam param = new RegisterParam();
    assertNull(param.getEmail());
    assertNull(param.getUsername());
    assertNull(param.getPassword());
  }

  @Test
  public void should_have_all_fields_accessible() {
    RegisterParam param = new RegisterParam("a@b.com", "user1", "pass1");
    assertNotNull(param.getEmail());
    assertNotNull(param.getUsername());
    assertNotNull(param.getPassword());
  }
}
