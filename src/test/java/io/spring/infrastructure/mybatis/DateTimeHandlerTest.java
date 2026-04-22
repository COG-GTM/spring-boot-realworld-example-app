package io.spring.infrastructure.mybatis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateTimeHandlerTest {

  private DateTimeHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new DateTimeHandler();
  }

  @Test
  public void should_set_non_null_instant_as_timestamp() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);
    Instant instant = Instant.ofEpochMilli(1_700_000_000_000L);

    handler.setParameter(ps, 1, instant, JdbcType.TIMESTAMP);

    verify(ps).setTimestamp(eq(1), eq(Timestamp.from(instant)), any());
  }

  @Test
  public void should_set_null_timestamp_when_parameter_is_null() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    handler.setParameter(ps, 1, null, JdbcType.TIMESTAMP);

    verify(ps).setTimestamp(eq(1), eq(null), any());
  }

  @Test
  public void should_return_instant_from_result_set_by_column_name() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Instant instant = Instant.ofEpochMilli(1_700_000_000_000L);
    when(rs.getTimestamp(eq("created_at"), any())).thenReturn(Timestamp.from(instant));

    Instant result = handler.getResult(rs, "created_at");

    Assertions.assertEquals(instant, result);
  }

  @Test
  public void should_return_null_when_result_set_column_name_has_null() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(anyString(), any())).thenReturn(null);

    Assertions.assertNull(handler.getResult(rs, "created_at"));
  }

  @Test
  public void should_return_instant_from_result_set_by_column_index() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Instant instant = Instant.ofEpochMilli(1_700_000_000_000L);
    when(rs.getTimestamp(eq(1), any())).thenReturn(Timestamp.from(instant));

    Instant result = handler.getResult(rs, 1);

    Assertions.assertEquals(instant, result);
  }

  @Test
  public void should_return_null_when_result_set_column_index_has_null() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(anyInt(), any())).thenReturn(null);

    Assertions.assertNull(handler.getResult(rs, 1));
  }

  @Test
  public void should_return_instant_from_callable_statement() throws SQLException {
    CallableStatement cs = mock(CallableStatement.class);
    Instant instant = Instant.ofEpochMilli(1_700_000_000_000L);
    when(cs.getTimestamp(eq(1), any())).thenReturn(Timestamp.from(instant));

    Instant result = handler.getResult(cs, 1);

    Assertions.assertEquals(instant, result);
  }

  @Test
  public void should_return_null_when_callable_statement_has_null() throws SQLException {
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getTimestamp(anyInt(), any())).thenReturn(null);

    Assertions.assertNull(handler.getResult(cs, 1));
  }
}
