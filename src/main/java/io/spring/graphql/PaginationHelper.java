package io.spring.graphql;

import graphql.execution.DataFetcherResult;
import graphql.relay.DefaultConnectionCursor;
import graphql.relay.DefaultPageInfo;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.DateTimeCursor;
import io.spring.application.Node;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.joda.time.DateTime;

public class PaginationHelper {

  @FunctionalInterface
  public interface ServiceCall<T extends Node> {
    CursorPager<T> execute(CursorPageParameter<DateTime> pageParameter);
  }

  public static <T extends Node, E, C> DataFetcherResult<C> buildPaginatedResult(
      Integer first,
      String after,
      Integer last,
      String before,
      ServiceCall<T> serviceCall,
      Function<T, E> edgeBuilder,
      BiFunction<graphql.relay.PageInfo, List<E>, C> connectionBuilder,
      Function<T, String> contextKeyExtractor) {

    if (first == null && last == null) {
      throw new IllegalArgumentException("first 和 last 必须只存在一个");
    }

    CursorPageParameter<DateTime> pageParameter;
    if (first != null) {
      pageParameter = new CursorPageParameter<>(DateTimeCursor.parse(after), first, Direction.NEXT);
    } else {
      pageParameter = new CursorPageParameter<>(DateTimeCursor.parse(before), last, Direction.PREV);
    }

    CursorPager<T> pager = serviceCall.execute(pageParameter);

    graphql.relay.PageInfo pageInfo = buildPageInfo(pager);

    List<E> edges =
        pager.getData().stream().map(edgeBuilder).collect(Collectors.toList());

    C connection = connectionBuilder.apply(pageInfo, edges);

    Map<String, T> localContext =
        pager.getData().stream().collect(Collectors.toMap(contextKeyExtractor, item -> item));

    return DataFetcherResult.<C>newResult().data(connection).localContext(localContext).build();
  }

  public static <T extends Node> DefaultPageInfo buildPageInfo(CursorPager<T> pager) {
    return new DefaultPageInfo(
        pager.getStartCursor() == null
            ? null
            : new DefaultConnectionCursor(pager.getStartCursor().toString()),
        pager.getEndCursor() == null
            ? null
            : new DefaultConnectionCursor(pager.getEndCursor().toString()),
        pager.hasPrevious(),
        pager.hasNext());
  }
}
