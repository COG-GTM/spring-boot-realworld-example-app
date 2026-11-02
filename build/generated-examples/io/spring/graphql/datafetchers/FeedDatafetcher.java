package io.spring.graphql.datafetchers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetchingEnvironment;
import io.spring.graphql.types.ArticlesConnection;

@DgsComponent
public class FeedDatafetcher {
  @DgsData(
      parentType = "Query",
      field = "feed"
  )
  public ArticlesConnection getFeed(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
