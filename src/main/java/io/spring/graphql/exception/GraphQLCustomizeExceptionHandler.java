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
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GraphQLCustomizeExceptionHandler
    implements DataFetcherExceptionHandler {

  private final DefaultDataFetcherExceptionHandler
      defaultHandler =
          new DefaultDataFetcherExceptionHandler();

  @Override
  public CompletableFuture<
          DataFetcherExceptionHandlerResult>
      handleException(
          DataFetcherExceptionHandlerParameters
              handlerParameters) {
    Throwable ex =
        handlerParameters.getException();
    if (ex
        instanceof InvalidAuthenticationException) {
      GraphQLError graphqlError =
          TypedGraphQLError.newBuilder()
              .errorType(
                  ErrorType.UNAUTHENTICATED)
              .message(ex.getMessage())
              .path(
                  handlerParameters.getPath())
              .build();
      DataFetcherExceptionHandlerResult result =
          DataFetcherExceptionHandlerResult
              .newResult()
              .error(graphqlError)
              .build();
      return CompletableFuture
          .completedFuture(result);
    } else if (ex
        instanceof
            ConstraintViolationException) {
      return handleConstraintViolation(
          (ConstraintViolationException) ex,
          handlerParameters);
    } else {
      return defaultHandler.handleException(
          handlerParameters);
    }
  }

  private CompletableFuture<
          DataFetcherExceptionHandlerResult>
      handleConstraintViolation(
          ConstraintViolationException cve,
          DataFetcherExceptionHandlerParameters
              params) {
    List<FieldErrorResource> errors =
        new ArrayList<>();
    for (ConstraintViolation<?> v :
        cve.getConstraintViolations()) {
      FieldErrorResource fer =
          new FieldErrorResource(
              v.getRootBeanClass().getName(),
              getParam(
                  v.getPropertyPath()
                      .toString()),
              v.getConstraintDescriptor()
                  .getAnnotation()
                  .annotationType()
                  .getSimpleName(),
              v.getMessage());
      errors.add(fer);
    }
    GraphQLError graphqlError =
        TypedGraphQLError.newBadRequestBuilder()
            .message(cve.getMessage())
            .path(params.getPath())
            .extensions(errorsToMap(errors))
            .build();
    DataFetcherExceptionHandlerResult result =
        DataFetcherExceptionHandlerResult
            .newResult()
            .error(graphqlError)
            .build();
    return CompletableFuture
        .completedFuture(result);
  }

  public static Error getErrorsAsData(
      ConstraintViolationException cve) {
    List<FieldErrorResource> errors =
        new ArrayList<>();
    for (ConstraintViolation<?> v :
        cve.getConstraintViolations()) {
      FieldErrorResource fer =
          new FieldErrorResource(
              v.getRootBeanClass().getName(),
              getParam(
                  v.getPropertyPath()
                      .toString()),
              v.getConstraintDescriptor()
                  .getAnnotation()
                  .annotationType()
                  .getSimpleName(),
              v.getMessage());
      errors.add(fer);
    }
    Map<String, List<String>> errorMap =
        new HashMap<>();
    for (FieldErrorResource fer : errors) {
      if (!errorMap.containsKey(
          fer.getField())) {
        errorMap.put(
            fer.getField(),
            new ArrayList<>());
      }
      errorMap
          .get(fer.getField())
          .add(fer.getMessage());
    }
    List<ErrorItem> errorItems =
        errorMap.entrySet().stream()
            .map(
                kv ->
                    ErrorItem.newBuilder()
                        .key(kv.getKey())
                        .value(kv.getValue())
                        .build())
            .collect(Collectors.toList());
    return Error.newBuilder()
        .message("BAD_REQUEST")
        .errors(errorItems)
        .build();
  }

  private static String getParam(String s) {
    String[] splits = s.split("\\.");
    if (splits.length == 1) {
      return s;
    } else {
      return String.join(
          ".",
          Arrays.copyOfRange(
              splits, 2, splits.length));
    }
  }

  private static Map<String, Object>
      errorsToMap(
          List<FieldErrorResource> errors) {
    Map<String, Object> json =
        new HashMap<>();
    for (FieldErrorResource fer : errors) {
      if (!json.containsKey(fer.getField())) {
        json.put(
            fer.getField(),
            new ArrayList<>());
      }
      ((List)
              json.get(fer.getField()))
          .add(fer.getMessage());
    }
    return json;
  }
}
