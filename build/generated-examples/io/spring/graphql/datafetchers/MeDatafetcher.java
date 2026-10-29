package io.spring.graphql.datafetchers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetchingEnvironment;
import io.spring.graphql.types.User;

@DgsComponent
public class MeDatafetcher {
  @DgsData(
      parentType = "Query",
      field = "me"
  )
  public User getMe(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
