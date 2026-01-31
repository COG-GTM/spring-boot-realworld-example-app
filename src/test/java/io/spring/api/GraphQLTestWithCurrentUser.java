package io.spring.api;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class GraphQLTestWithCurrentUser extends TestWithCurrentUser {
  
  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    UsernamePasswordAuthenticationToken authentication = 
      new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
