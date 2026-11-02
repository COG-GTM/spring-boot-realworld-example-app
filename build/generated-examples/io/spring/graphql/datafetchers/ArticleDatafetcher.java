package io.spring.graphql.datafetchers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetchingEnvironment;
import io.spring.graphql.types.Article;

@DgsComponent
public class ArticleDatafetcher {
  @DgsData(
      parentType = "Query",
      field = "article"
  )
  public Article getArticle(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
