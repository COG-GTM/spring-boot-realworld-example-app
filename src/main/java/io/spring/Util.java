package io.spring;

public final class Util {
  private Util() {}

  public static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
