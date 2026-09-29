package io.spring.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
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

/**
 * Records a count and duration for every MyBatis mapped statement executed through an {@link
 * Executor}.
 *
 * <ul>
 *   <li>{@code mybatis.queries} (counter): number of statements executed.
 *   <li>{@code mybatis.query.duration} (timer): time spent executing each statement.
 * </ul>
 *
 * <p>Both meters are tagged with {@code statement} (the mapped statement id, e.g. {@code
 * io.spring.infrastructure.mybatis.mapper.UserMapper.findByUsername}), {@code command} ({@code
 * SELECT}, {@code INSERT}, {@code UPDATE}, {@code DELETE}) and {@code outcome} ({@code SUCCESS} or
 * {@code ERROR}).
 */
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "update",
      args = {MappedStatement.class, Object.class}),
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
      })
})
public class MyBatisMetricsInterceptor implements Interceptor {
  public static final String QUERY_COUNT_METRIC = "mybatis.queries";
  public static final String QUERY_DURATION_METRIC = "mybatis.query.duration";

  private final MeterRegistry registry;

  public MyBatisMetricsInterceptor(MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
    long start = System.nanoTime();
    String outcome = "SUCCESS";
    try {
      return invocation.proceed();
    } catch (Throwable e) {
      outcome = "ERROR";
      throw e;
    } finally {
      long elapsed = System.nanoTime() - start;
      Tags tags =
          Tags.of(
              "statement", statement.getId(),
              "command", statement.getSqlCommandType().name(),
              "outcome", outcome);
      Counter.builder(QUERY_COUNT_METRIC)
          .description("Number of MyBatis mapped statements executed")
          .tags(tags)
          .register(registry)
          .increment();
      Timer.builder(QUERY_DURATION_METRIC)
          .description("Execution time of MyBatis mapped statements")
          .tags(tags)
          .register(registry)
          .record(elapsed, TimeUnit.NANOSECONDS);
    }
  }

  @Override
  public void setProperties(Properties properties) {}
}
