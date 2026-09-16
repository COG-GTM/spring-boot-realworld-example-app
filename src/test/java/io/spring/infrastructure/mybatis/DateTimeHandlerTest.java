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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateTimeHandlerTest {

  private static final long MILLIS = 1500000000000L;

  private DateTimeHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new DateTimeHandler();
  }

  @Test
  public void should_set_timestamp_parameter() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);
    handler.setParameter(ps, 1, new DateTime(MILLIS), JdbcType.TIMESTAMP);
    verify(ps).setTimestamp(eq(1), eq(new Timestamp(MILLIS)), any(Calendar.class));
  }

  @Test
  public void should_set_null_parameter() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);
    handler.setParameter(ps, 1, null, JdbcType.TIMESTAMP);
    verify(ps).setTimestamp(eq(1), isNull(), any(Calendar.class));
  }

  @Test
  public void should_get_result_by_column_name() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(eq("created_at"), any(Calendar.class))).thenReturn(new Timestamp(MILLIS));
    assertEquals(MILLIS, handler.getResult(rs, "created_at").getMillis());
  }

  @Test
  public void should_get_result_by_column_index() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(eq(2), any(Calendar.class))).thenReturn(new Timestamp(MILLIS));
    assertEquals(MILLIS, handler.getResult(rs, 2).getMillis());
  }

  @Test
  public void should_get_result_from_callable_statement() throws Exception {
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getTimestamp(eq(3), any(Calendar.class))).thenReturn(new Timestamp(MILLIS));
    assertEquals(MILLIS, handler.getResult(cs, 3).getMillis());
  }

  @Test
  public void should_return_null_for_null_timestamps() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    CallableStatement cs = mock(CallableStatement.class);
    assertNull(handler.getResult(rs, "created_at"));
    assertNull(handler.getResult(rs, 1));
    assertNull(handler.getResult(cs, 1));
  }
}
