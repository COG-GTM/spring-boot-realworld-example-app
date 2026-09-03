package io.spring.api.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import tools.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = ErrorResourceSerializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Getter
@JsonRootName("errors")
public class ErrorResource {
  private List<FieldErrorResource> fieldErrors;

  public ErrorResource(List<FieldErrorResource> fieldErrorResources) {
    this.fieldErrors = fieldErrorResources;
  }
}
