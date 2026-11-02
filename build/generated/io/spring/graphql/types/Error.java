package io.spring.graphql.types;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NONE
)
public class Error implements UserResult {
  private String message;

  private List<ErrorItem> errors;

  public Error() {
  }

  public Error(String message, List<ErrorItem> errors) {
    this.message = message;
    this.errors = errors;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<ErrorItem> getErrors() {
    return errors;
  }

  public void setErrors(List<ErrorItem> errors) {
    this.errors = errors;
  }

  @Override
  public String toString() {
    return "Error{" + "message='" + message + "'," +"errors='" + errors + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Error that = (Error) o;
        return java.util.Objects.equals(message, that.message) &&
                            java.util.Objects.equals(errors, that.errors);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(message, errors);
  }

  public static io.spring.graphql.types.Error.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String message;

    private List<ErrorItem> errors;

    public Error build() {
                  io.spring.graphql.types.Error result = new io.spring.graphql.types.Error();
                      result.message = this.message;
          result.errors = this.errors;
                      return result;
    }

    public io.spring.graphql.types.Error.Builder message(String message) {
      this.message = message;
      return this;
    }

    public io.spring.graphql.types.Error.Builder errors(List<ErrorItem> errors) {
      this.errors = errors;
      return this;
    }
  }
}
