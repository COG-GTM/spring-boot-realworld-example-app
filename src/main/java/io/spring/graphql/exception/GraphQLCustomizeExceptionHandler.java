package io.spring.graphql.exception;

import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler;
import com.netflix.graphql.types.errors.ErrorType;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import io.spring.api.exception.FieldErrorResource;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GraphQLCustomizeExceptionHandler implements DataFetcherExceptionHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(GraphQLCustomizeExceptionHandler.class);

  private final DefaultDataFetcherExceptionHandler defaultHandler =
      new DefaultDataFetcherExceptionHandler();

  @Override
  public DataFetcherExceptionHandlerResult onException(
      DataFetcherExceptionHandlerParameters handlerParameters) {
    Throwable exception = handlerParameters.getException();

    if (exception instanceof InvalidAuthenticationException) {
      logger.warn(
          "Authentication failed for path {}: {}",
          handlerParameters.getPath(),
          exception.getMessage());
      GraphQLError graphqlError =
          TypedGraphQLError.newBuilder()
              .errorType(ErrorType.UNAUTHENTICATED)
              .message(
                  exception.getMessage() != null
                      ? exception.getMessage()
                      : "Authentication required")
              .path(handlerParameters.getPath())
              .build();
      return DataFetcherExceptionHandlerResult.newResult().error(graphqlError).build();
    } else if (exception instanceof ResourceNotFoundException) {
      logger.debug("Resource not found for path {}", handlerParameters.getPath());
      GraphQLError graphqlError =
          TypedGraphQLError.newNotFoundBuilder()
              .message("The requested resource was not found")
              .path(handlerParameters.getPath())
              .build();
      return DataFetcherExceptionHandlerResult.newResult().error(graphqlError).build();
    } else if (exception instanceof NoAuthorizationException) {
      logger.warn(
          "Authorization denied for path {}: {}",
          handlerParameters.getPath(),
          exception.getMessage());
      GraphQLError graphqlError =
          TypedGraphQLError.newPermissionDeniedBuilder()
              .message("You are not authorized to perform this action")
              .path(handlerParameters.getPath())
              .build();
      return DataFetcherExceptionHandlerResult.newResult().error(graphqlError).build();
    } else if (exception instanceof IllegalArgumentException) {
      logger.warn(
          "Invalid argument for path {}: {}",
          handlerParameters.getPath(),
          exception.getMessage());
      GraphQLError graphqlError =
          TypedGraphQLError.newBadRequestBuilder()
              .message(
                  exception.getMessage() != null ? exception.getMessage() : "Invalid argument")
              .path(handlerParameters.getPath())
              .build();
      return DataFetcherExceptionHandlerResult.newResult().error(graphqlError).build();
    } else if (exception instanceof ConstraintViolationException) {
      logger.warn("Validation failed for path {}", handlerParameters.getPath());
      List<FieldErrorResource> errors = new ArrayList<>();
      for (ConstraintViolation<?> violation :
          ((ConstraintViolationException) exception).getConstraintViolations()) {
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
              .message("Validation failed")
              .path(handlerParameters.getPath())
              .extensions(errorsToMap(errors))
              .build();
      return DataFetcherExceptionHandlerResult.newResult().error(graphqlError).build();
    } else {
      logger.error(
          "Unexpected error for path {}: {}",
          handlerParameters.getPath(),
          exception.getMessage(),
          exception);
      return defaultHandler.onException(handlerParameters);
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
      if (!errorMap.containsKey(fieldErrorResource.getField())) {
        errorMap.put(fieldErrorResource.getField(), new ArrayList<>());
      }
      errorMap.get(fieldErrorResource.getField()).add(fieldErrorResource.getMessage());
    }
    List<ErrorItem> errorItems =
        errorMap.entrySet().stream()
            .map(kv -> ErrorItem.newBuilder().key(kv.getKey()).value(kv.getValue()).build())
            .collect(Collectors.toList());
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
      if (!json.containsKey(fieldErrorResource.getField())) {
        json.put(fieldErrorResource.getField(), new ArrayList<>());
      }
      ((List) json.get(fieldErrorResource.getField())).add(fieldErrorResource.getMessage());
    }
    return json;
  }
}
