package io.spring.infrastructure.mybatis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.apache.ibatis.type.JdbcType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateTimeHandlerTest {

  private DateTimeHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DateTimeHandler();
  }

  @Test
  public void should_set_timestamp_parameter() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);
    DateTime dt = new DateTime(2023, 6, 15, 10, 30);
    handler.setParameter(ps, 1, dt, JdbcType.TIMESTAMP);
    verify(ps).setTimestamp(eq(1), any(Timestamp.class), any());
  }

  @Test
  public void should_set_null_parameter_when_datetime_is_null() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);
    handler.setParameter(ps, 1, null, JdbcType.TIMESTAMP);
    verify(ps).setTimestamp(eq(1), isNull(), any());
  }

  @Test
  public void should_get_result_by_column_name() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Timestamp ts = new Timestamp(1686825000000L);
    when(rs.getTimestamp(eq("created_at"), any())).thenReturn(ts);
    DateTime result = handler.getResult(rs, "created_at");
    assertNotNull(result);
    assertEquals(ts.getTime(), result.getMillis());
  }

  @Test
  public void should_return_null_for_null_column_name_result() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(eq("created_at"), any())).thenReturn(null);
    DateTime result = handler.getResult(rs, "created_at");
    assertNull(result);
  }

  @Test
  public void should_get_result_by_column_index() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Timestamp ts = new Timestamp(1686825000000L);
    when(rs.getTimestamp(eq(1), any())).thenReturn(ts);
    DateTime result = handler.getResult(rs, 1);
    assertNotNull(result);
    assertEquals(ts.getTime(), result.getMillis());
  }

  @Test
  public void should_return_null_for_null_column_index_result() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(eq(1), any())).thenReturn(null);
    DateTime result = handler.getResult(rs, 1);
    assertNull(result);
  }

  @Test
  public void should_get_result_from_callable_statement() throws SQLException {
    CallableStatement cs = mock(CallableStatement.class);
    Timestamp ts = new Timestamp(1686825000000L);
    when(cs.getTimestamp(eq(1), any())).thenReturn(ts);
    DateTime result = handler.getResult(cs, 1);
    assertNotNull(result);
    assertEquals(ts.getTime(), result.getMillis());
  }

  @Test
  public void should_return_null_from_callable_statement_when_null() throws SQLException {
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getTimestamp(eq(1), any())).thenReturn(null);
    DateTime result = handler.getResult(cs, 1);
    assertNull(result);
  }
}
