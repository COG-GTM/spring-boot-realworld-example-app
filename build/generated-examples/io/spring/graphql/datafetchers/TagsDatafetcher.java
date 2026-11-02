package io.spring.graphql.datafetchers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetchingEnvironment;
import java.lang.String;
import java.util.List;

@DgsComponent
public class TagsDatafetcher {
  @DgsData(
      parentType = "Query",
      field = "tags"
  )
  public List<String> getTags(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
