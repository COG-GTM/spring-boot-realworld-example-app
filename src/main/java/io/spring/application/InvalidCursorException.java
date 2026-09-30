package io.spring.application;

public class InvalidCursorException extends IllegalArgumentException {
  public InvalidCursorException() {
    super("Invalid pagination cursor");
  }
}
