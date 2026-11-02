package io.spring.graphql.datafetchers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetchingEnvironment;
import io.spring.graphql.types.ArticlesConnection;

@DgsComponent
public class ArticlesDatafetcher {
  @DgsData(
      parentType = "Query",
      field = "articles"
  )
  public ArticlesConnection getArticles(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
