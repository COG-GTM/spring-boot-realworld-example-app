package io.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;

@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
public class RealWorldApplication {

  public static void main(String[] args) {
    SpringApplication.run(RealWorldApplication.class, args);
  }
}
