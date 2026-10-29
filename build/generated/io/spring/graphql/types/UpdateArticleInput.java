package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class UpdateArticleInput {
  private String body;

  private String description;

  private String title;

  public UpdateArticleInput() {
  }

  public UpdateArticleInput(String body, String description, String title) {
    this.body = body;
    this.description = description;
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return "UpdateArticleInput{" + "body='" + body + "'," +"description='" + description + "'," +"title='" + title + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateArticleInput that = (UpdateArticleInput) o;
        return java.util.Objects.equals(body, that.body) &&
                            java.util.Objects.equals(description, that.description) &&
                            java.util.Objects.equals(title, that.title);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(body, description, title);
  }

  public static io.spring.graphql.types.UpdateArticleInput.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String body;

    private String description;

    private String title;

    public UpdateArticleInput build() {
                  io.spring.graphql.types.UpdateArticleInput result = new io.spring.graphql.types.UpdateArticleInput();
                      result.body = this.body;
          result.description = this.description;
          result.title = this.title;
                      return result;
    }

    public io.spring.graphql.types.UpdateArticleInput.Builder body(String body) {
      this.body = body;
      return this;
    }

    public io.spring.graphql.types.UpdateArticleInput.Builder description(String description) {
      this.description = description;
      return this;
    }

    public io.spring.graphql.types.UpdateArticleInput.Builder title(String title) {
      this.title = title;
      return this;
    }
  }
}
