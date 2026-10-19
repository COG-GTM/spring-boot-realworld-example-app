package io.spring.infrastructure.mybatis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;

public class MyBatisLogLevelTest {

  private static final String[] SENSITIVE_LOGGERS = {
    "io.spring.infrastructure.mybatis.mapper.UserMapper",
    "io.spring.infrastructure.mybatis.mapper",
    "io.spring.infrastructure.mybatis.readservice.ArticleReadService",
    "io.spring.infrastructure.mybatis.readservice.UserReadService"
  };

  @SpringBootConfiguration
  static class EmptyConfig {}

  @AfterEach
  public void resetLevels() {
    LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
    loggingSystem.setLogLevel("io.spring.infrastructure.mybatis", null);
    loggingSystem.setLogLevel("io.spring.infrastructure.mybatis.mapper", null);
    loggingSystem.setLogLevel(
        "io.spring.infrastructure.mybatis.readservice.ArticleReadService", null);
  }

  private ConfigurableApplicationContext start(String... profiles) {
    return new SpringApplicationBuilder(EmptyConfig.class)
        .web(WebApplicationType.NONE)
        .profiles(profiles)
        .run();
  }

  @Test
  public void should_not_log_mybatis_statements_by_default() {
    try (ConfigurableApplicationContext ignored = start()) {
      for (String name : SENSITIVE_LOGGERS) {
        assertFalse(LoggerFactory.getLogger(name).isDebugEnabled(), name + " is DEBUG");
      }
    }
  }

  @Test
  public void should_keep_mybatis_at_info_even_when_root_is_debug() {
    try (ConfigurableApplicationContext ignored =
        new SpringApplicationBuilder(EmptyConfig.class)
            .web(WebApplicationType.NONE)
            .properties("logging.level.root=DEBUG")
            .run()) {
      for (String name : SENSITIVE_LOGGERS) {
        assertFalse(LoggerFactory.getLogger(name).isDebugEnabled(), name + " is DEBUG");
      }
    } finally {
      LoggingSystem.get(getClass().getClassLoader())
          .setLogLevel(LoggingSystem.ROOT_LOGGER_NAME, LogLevel.INFO);
    }
  }

  @Test
  public void should_log_mybatis_statements_only_with_dev_profile() {
    try (ConfigurableApplicationContext ignored = start("dev")) {
      assertTrue(
          LoggerFactory.getLogger("io.spring.infrastructure.mybatis.mapper.UserMapper")
              .isDebugEnabled());
    }
  }
}
