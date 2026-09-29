package io.spring.querycount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/** Records every MyBatis statement executed, keyed by mapped statement id. */
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "query",
      args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(
      type = Executor.class,
      method = "query",
      args = {
        MappedStatement.class,
        Object.class,
        RowBounds.class,
        ResultHandler.class,
        CacheKey.class,
        BoundSql.class
      }),
  @Signature(
      type = Executor.class,
      method = "update",
      args = {MappedStatement.class, Object.class})
})
public class QueryCounter implements Interceptor {
  private final List<String> statements = Collections.synchronizedList(new ArrayList<>());

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
    statements.add(ms.getId());
    return invocation.proceed();
  }

  public void reset() {
    statements.clear();
  }

  public int count() {
    return statements.size();
  }

  public Map<String, Integer> countsByStatement() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    synchronized (statements) {
      for (String id : statements) {
        String shortId = id.substring(id.lastIndexOf('.', id.lastIndexOf('.') - 1) + 1);
        counts.merge(shortId, 1, Integer::sum);
      }
    }
    return counts;
  }
}
