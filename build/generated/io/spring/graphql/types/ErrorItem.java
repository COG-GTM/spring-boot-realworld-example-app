package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class ErrorItem {
  private String key;

  private List<String> value;

  public ErrorItem() {
  }

  public ErrorItem(String key, List<String> value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<String> getValue() {
    return value;
  }

  public void setValue(List<String> value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "ErrorItem{" + "key='" + key + "'," +"value='" + value + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorItem that = (ErrorItem) o;
        return java.util.Objects.equals(key, that.key) &&
                            java.util.Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(key, value);
  }

  public static io.spring.graphql.types.ErrorItem.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String key;

    private List<String> value;

    public ErrorItem build() {
                  io.spring.graphql.types.ErrorItem result = new io.spring.graphql.types.ErrorItem();
                      result.key = this.key;
          result.value = this.value;
                      return result;
    }

    public io.spring.graphql.types.ErrorItem.Builder key(String key) {
      this.key = key;
      return this;
    }

    public io.spring.graphql.types.ErrorItem.Builder value(List<String> value) {
      this.value = value;
      return this;
    }
  }
}
