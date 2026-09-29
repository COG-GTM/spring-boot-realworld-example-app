package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.spring.application.CursorPager;
import io.spring.application.PageCursor;
import java.util.List;
import lombok.Getter;

@Getter
public class ArticleCursorDataList {
  @JsonProperty("articles")
  private final List<ArticleData> articleDatas;

  @JsonProperty("pageInfo")
  private final PageInfo pageInfo;

  public ArticleCursorDataList(CursorPager<ArticleData> pager) {
    this.articleDatas = pager.getData();
    this.pageInfo =
        new PageInfo(
            cursorToString(pager.getStartCursor()),
            cursorToString(pager.getEndCursor()),
            pager.hasNext(),
            pager.hasPrevious());
  }

  private static String cursorToString(PageCursor cursor) {
    return cursor == null ? null : cursor.toString();
  }

  @Getter
  public static class PageInfo {
    private final String startCursor;
    private final String endCursor;
    private final boolean hasNextPage;
    private final boolean hasPreviousPage;

    PageInfo(String startCursor, String endCursor, boolean hasNextPage, boolean hasPreviousPage) {
      this.startCursor = startCursor;
      this.endCursor = endCursor;
      this.hasNextPage = hasNextPage;
      this.hasPreviousPage = hasPreviousPage;
    }
  }
}
