package io.spring.graphql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.InputArgument;
import com.netflix.graphql.dgs.context.DgsContext;
import com.netflix.graphql.dgs.internal.DgsWebMvcRequestData;
import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.api.security.LoginThrottle;
import io.spring.application.user.LoginService;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UpdateUserParam;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.graphql.DgsConstants.MUTATION;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@DgsComponent
@AllArgsConstructor
public class UserMutation {

  private LoginService loginService;
  private LoginThrottle loginThrottle;
  private UserService userService;

  @DgsData(parentType = MUTATION.TYPE_NAME, field = MUTATION.CreateUser)
  public DataFetcherResult<UserResult> createUser(@InputArgument("input") CreateUserInput input) {
    RegisterParam registerParam =
        new RegisterParam(input.getEmail(), input.getUsername(), input.getPassword());
    User user;
    try {
      user = userService.createUser(registerParam);
    } catch (ConstraintViolationException cve) {
      return DataFetcherResult.<UserResult>newResult()
          .data(GraphQLCustomizeExceptionHandler.getErrorsAsData(cve))
          .build();
    }

    return DataFetcherResult.<UserResult>newResult()
        .data(UserPayload.newBuilder().build())
        .localContext(user)
        .build();
  }

  @DgsData(parentType = MUTATION.TYPE_NAME, field = MUTATION.Login)
  public DataFetcherResult<UserPayload> login(
      @InputArgument("password") String password,
      @InputArgument("email") String email,
      DgsDataFetchingEnvironment dfe) {
    Optional<User> optional =
        loginThrottle.attempt(
            clientAddress(dfe), email, () -> loginService.authenticate(email, password));
    if (optional.isPresent()) {
      return DataFetcherResult.<UserPayload>newResult()
          .data(UserPayload.newBuilder().build())
          .localContext(optional.get())
          .build();
    } else {
      throw new InvalidAuthenticationException();
    }
  }

  @DgsData(parentType = MUTATION.TYPE_NAME, field = MUTATION.UpdateUser)
  public DataFetcherResult<UserPayload> updateUser(
      @InputArgument("changes") UpdateUserInput updateUserInput) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof AnonymousAuthenticationToken
        || authentication.getPrincipal() == null) {
      return null;
    }
    io.spring.core.user.User currentUser = (io.spring.core.user.User) authentication.getPrincipal();
    UpdateUserParam param =
        UpdateUserParam.builder()
            .username(updateUserInput.getUsername())
            .email(updateUserInput.getEmail())
            .bio(updateUserInput.getBio())
            .password(updateUserInput.getPassword())
            .image(updateUserInput.getImage())
            .build();

    userService.updateUser(new UpdateUserCommand(currentUser, param));
    return DataFetcherResult.<UserPayload>newResult()
        .data(UserPayload.newBuilder().build())
        .localContext(currentUser)
        .build();
  }

  private static String clientAddress(DgsDataFetchingEnvironment dfe) {
    if (dfe == null || !(dfe.getContext() instanceof DgsContext)) {
      return null;
    }
    Object requestData = ((DgsContext) dfe.getContext()).getRequestData();
    if (!(requestData instanceof DgsWebMvcRequestData)) {
      return null;
    }
    WebRequest webRequest = ((DgsWebMvcRequestData) requestData).getWebRequest();
    if (!(webRequest instanceof ServletWebRequest)) {
      return null;
    }
    return ((ServletWebRequest) webRequest).getRequest().getRemoteAddr();
  }
}
