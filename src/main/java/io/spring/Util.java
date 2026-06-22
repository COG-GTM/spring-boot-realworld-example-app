package io.spring;

public class Util {
  private Util() {}

  public static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
