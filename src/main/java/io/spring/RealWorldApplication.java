package io.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RealWorldApplication {

  public static void main(String[] args) {
    SpringApplication.run(RealWorldApplication.class, args);
  }
}
