package io.spring.api.ratelimit;

import io.spring.core.ratelimit.RateLimitAction;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a controller method as subject to the configured limit for {@link #value()}. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
  RateLimitAction value();
}
