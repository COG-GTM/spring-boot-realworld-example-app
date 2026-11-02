package io.spring.graphql.datafetchers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetchingEnvironment;
import io.spring.graphql.types.ProfilePayload;

@DgsComponent
public class ProfileDatafetcher {
  @DgsData(
      parentType = "Query",
      field = "profile"
  )
  public ProfilePayload getProfile(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
