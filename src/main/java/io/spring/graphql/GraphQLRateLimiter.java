package io.spring.graphql;

import com.netflix.graphql.dgs.context.DgsContext;
import com.netflix.graphql.dgs.internal.DgsRequestData;
import com.netflix.graphql.dgs.internal.DgsWebMvcRequestData;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.ratelimit.RateLimitPolicy;
import io.spring.api.ratelimit.RateLimitService;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;

@Component
@AllArgsConstructor
public class GraphQLRateLimiter {
  private RateLimitService rateLimitService;

  public void acquire(RateLimitPolicy policy, DataFetchingEnvironment env) {
    rateLimitService.acquire(policy, servletRequest(env));
  }

  private static HttpServletRequest servletRequest(DataFetchingEnvironment env) {
    DgsRequestData requestData = DgsContext.getRequestData(env);
    if (requestData instanceof DgsWebMvcRequestData) {
      WebRequest webRequest = ((DgsWebMvcRequestData) requestData).getWebRequest();
      if (webRequest instanceof NativeWebRequest) {
        return ((NativeWebRequest) webRequest).getNativeRequest(HttpServletRequest.class);
      }
    }
    return null;
  }
}
