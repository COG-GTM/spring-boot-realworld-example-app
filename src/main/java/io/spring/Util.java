package io.spring;

import io.spring.core.util.StringUtils;

/**
 * General utility class.
 *
 * @deprecated Use {@link io.spring.core.util.StringUtils} instead for string operations.
 */
@Deprecated
public class Util {
  /**
   * Checks if a string is null or empty.
   *
   * @deprecated Use {@link StringUtils#isEmpty(String)} instead.
   */
  @Deprecated
  public static boolean isEmpty(String value) {
    return StringUtils.isEmpty(value);
  }
}
