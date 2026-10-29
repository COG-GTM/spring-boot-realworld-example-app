package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class DeletionStatus {
  private boolean success;

  public DeletionStatus() {
  }

  public DeletionStatus(boolean success) {
    this.success = success;
  }

  public boolean getSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    return "DeletionStatus{" + "success='" + success + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeletionStatus that = (DeletionStatus) o;
        return success == that.success;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(success);
  }

  public static io.spring.graphql.types.DeletionStatus.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private boolean success;

    public DeletionStatus build() {
      io.spring.graphql.types.DeletionStatus result = new io.spring.graphql.types.DeletionStatus();
          result.success = this.success;
          return result;
    }

    public io.spring.graphql.types.DeletionStatus.Builder success(boolean success) {
      this.success = success;
      return this;
    }
  }
}
