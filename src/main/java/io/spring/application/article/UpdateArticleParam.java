package io.spring.application.article;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonRootName("article")
public class UpdateArticleParam {
  @Builder.Default private String title = "";
  @Builder.Default private String body = "";
  @Builder.Default private String description = "";

  public boolean hasTitle() {
    return title != null && !title.isEmpty();
  }

  public boolean hasBody() {
    return body != null && !body.isEmpty();
  }

  public boolean hasDescription() {
    return description != null && !description.isEmpty();
  }
}
