package io.spring.graphql.exception;

import com.netflix.graphql.types.errors.ErrorType;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import io.spring.api.exception.FieldErrorResource;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

@Component
public class GraphQLCustomizeExceptionHandler implements DataFetcherExceptionHandler {

  private final SimpleDataFetcherExceptionHandler defaultHandler =
      new SimpleDataFetcherExceptionHandler();

  @Override
  public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
      DataFetcherExceptionHandlerParameters handlerParameters) {
    if (handlerParameters.getException() instanceof InvalidAuthenticationException e) {
      GraphQLError graphqlError =
          TypedGraphQLError.newBuilder()
              .errorType(ErrorType.UNAUTHENTICATED)
              .message(e.getMessage())
              .path(handlerParameters.getPath())
              .build();
      return CompletableFuture.completedFuture(
          DataFetcherExceptionHandlerResult.newResult().error(graphqlError).build());
    } else if (handlerParameters.getException() instanceof ConstraintViolationException cve) {
      List<FieldErrorResource> errors = new ArrayList<>();
      for (ConstraintViolation<?> violation : cve.getConstraintViolations()) {
        FieldErrorResource fieldErrorResource =
            new FieldErrorResource(
                violation.getRootBeanClass().getName(),
                getParam(violation.getPropertyPath().toString()),
                violation
                    .getConstraintDescriptor()
                    .getAnnotation()
                    .annotationType()
                    .getSimpleName(),
                violation.getMessage());
        errors.add(fieldErrorResource);
      }
      GraphQLError graphqlError =
          TypedGraphQLError.newBadRequestBuilder()
              .message(cve.getMessage())
              .path(handlerParameters.getPath())
              .extensions(errorsToMap(errors))
              .build();
      return CompletableFuture.completedFuture(
          DataFetcherExceptionHandlerResult.newResult().error(graphqlError).build());
    } else {
      return defaultHandler.handleException(handlerParameters);
    }
  }

  public static Error getErrorsAsData(ConstraintViolationException cve) {
    List<FieldErrorResource> errors = new ArrayList<>();
    for (ConstraintViolation<?> violation : cve.getConstraintViolations()) {
      FieldErrorResource fieldErrorResource =
          new FieldErrorResource(
              violation.getRootBeanClass().getName(),
              getParam(violation.getPropertyPath().toString()),
              violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
              violation.getMessage());
      errors.add(fieldErrorResource);
    }
    Map<String, List<String>> errorMap = new HashMap<>();
    for (FieldErrorResource fieldErrorResource : errors) {
      if (!errorMap.containsKey(fieldErrorResource.field())) {
        errorMap.put(fieldErrorResource.field(), new ArrayList<>());
      }
      errorMap.get(fieldErrorResource.field()).add(fieldErrorResource.message());
    }
    List<ErrorItem> errorItems =
        errorMap.entrySet().stream()
            .map(kv -> ErrorItem.newBuilder().key(kv.getKey()).value(kv.getValue()).build())
            .toList();
    return Error.newBuilder().message("BAD_REQUEST").errors(errorItems).build();
  }

  private static String getParam(String s) {
    String[] splits = s.split("\\.");
    if (splits.length == 1) {
      return s;
    } else {
      return String.join(".", Arrays.copyOfRange(splits, 2, splits.length));
    }
  }

  private static Map<String, Object> errorsToMap(List<FieldErrorResource> errors) {
    Map<String, Object> json = new HashMap<>();
    for (FieldErrorResource fieldErrorResource : errors) {
      if (!json.containsKey(fieldErrorResource.field())) {
        json.put(fieldErrorResource.field(), new ArrayList<>());
      }
      ((List) json.get(fieldErrorResource.field())).add(fieldErrorResource.message());
    }
    return json;
  }
}
