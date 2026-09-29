package io.spring.infrastructure.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import org.apache.ibatis.type.JdbcType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class DateTimeHandlerTest {
  private final DateTimeHandler handler = new DateTimeHandler();
  private final DateTime time = new DateTime(1_600_000_000_000L);

  @Test
  public void should_set_timestamp_or_null_parameter() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);

    handler.setParameter(ps, 1, time, JdbcType.TIMESTAMP);
    handler.setParameter(ps, 2, null, JdbcType.TIMESTAMP);

    verify(ps).setTimestamp(eq(1), eq(new Timestamp(time.getMillis())), any(Calendar.class));
    verify(ps).setTimestamp(eq(2), isNull(), any(Calendar.class));
  }

  @Test
  public void should_read_result_set_by_name_and_index() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(eq("present"), any(Calendar.class)))
        .thenReturn(new Timestamp(time.getMillis()));
    when(rs.getTimestamp(eq(1), any(Calendar.class))).thenReturn(new Timestamp(time.getMillis()));

    assertEquals(time.getMillis(), handler.getResult(rs, "present").getMillis());
    assertNull(handler.getResult(rs, "missing"));
    assertEquals(time.getMillis(), handler.getResult(rs, 1).getMillis());
    assertNull(handler.getResult(rs, 2));
  }

  @Test
  public void should_read_callable_statement() throws Exception {
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getTimestamp(eq(1), any(Calendar.class))).thenReturn(new Timestamp(time.getMillis()));

    assertEquals(time.getMillis(), handler.getResult(cs, 1).getMillis());
    assertNull(handler.getResult(cs, 2));
  }
}
