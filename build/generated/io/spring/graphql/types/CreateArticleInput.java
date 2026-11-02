package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class CreateArticleInput {
  private String body;

  private String description;

  private List<String> tagList;

  private String title;

  public CreateArticleInput() {
  }

  public CreateArticleInput(String body, String description, List<String> tagList, String title) {
    this.body = body;
    this.description = description;
    this.tagList = tagList;
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

  public List<String> getTagList() {
    return tagList;
  }

  public void setTagList(List<String> tagList) {
    this.tagList = tagList;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return "CreateArticleInput{" + "body='" + body + "'," +"description='" + description + "'," +"tagList='" + tagList + "'," +"title='" + title + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateArticleInput that = (CreateArticleInput) o;
        return java.util.Objects.equals(body, that.body) &&
                            java.util.Objects.equals(description, that.description) &&
                            java.util.Objects.equals(tagList, that.tagList) &&
                            java.util.Objects.equals(title, that.title);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(body, description, tagList, title);
  }

  public static io.spring.graphql.types.CreateArticleInput.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String body;

    private String description;

    private List<String> tagList;

    private String title;

    public CreateArticleInput build() {
                  io.spring.graphql.types.CreateArticleInput result = new io.spring.graphql.types.CreateArticleInput();
                      result.body = this.body;
          result.description = this.description;
          result.tagList = this.tagList;
          result.title = this.title;
                      return result;
    }

    public io.spring.graphql.types.CreateArticleInput.Builder body(String body) {
      this.body = body;
      return this;
    }

    public io.spring.graphql.types.CreateArticleInput.Builder description(String description) {
      this.description = description;
      return this;
    }

    public io.spring.graphql.types.CreateArticleInput.Builder tagList(List<String> tagList) {
      this.tagList = tagList;
      return this;
    }

    public io.spring.graphql.types.CreateArticleInput.Builder title(String title) {
      this.title = title;
      return this;
    }
  }
}
