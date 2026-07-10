package io.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = "DB", mode = ResourceAccessMode.READ_WRITE)
public class RealworldApplicationTests {

  @Test
  public void contextLoads() {}
}
