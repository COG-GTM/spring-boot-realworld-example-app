package io.spring.infrastructure.mybatis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.apache.ibatis.type.JdbcType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class DateTimeHandlerTest {

  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  private DateTimeHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new DateTimeHandler();
  }

  @Test
  public void should_set_timestamp_with_utc_calendar() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);
    DateTime time = new DateTime(1_600_000_000_000L);

    handler.setParameter(ps, 1, time, JdbcType.TIMESTAMP);

    ArgumentCaptor<Timestamp> timestamp = ArgumentCaptor.forClass(Timestamp.class);
    ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
    verify(ps).setTimestamp(eq(1), timestamp.capture(), calendar.capture());
    Assertions.assertEquals(time.getMillis(), timestamp.getValue().getTime());
    Assertions.assertEquals(UTC, calendar.getValue().getTimeZone());
  }

  @Test
  public void should_set_null_timestamp_when_parameter_is_null() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);

    handler.setParameter(ps, 2, null, JdbcType.TIMESTAMP);

    ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
    verify(ps).setTimestamp(eq(2), isNull(), calendar.capture());
    Assertions.assertEquals(UTC, calendar.getValue().getTimeZone());
  }

  @Test
  public void should_use_a_new_calendar_for_each_call() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);

    handler.setParameter(ps, 1, new DateTime(), JdbcType.TIMESTAMP);
    handler.setParameter(ps, 1, new DateTime(), JdbcType.TIMESTAMP);

    ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
    verify(ps, times(2)).setTimestamp(eq(1), any(Timestamp.class), calendar.capture());
    List<Calendar> calendars = calendar.getAllValues();
    Assertions.assertNotSame(calendars.get(0), calendars.get(1));
  }

  @Test
  public void should_read_result_by_column_name() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    long millis = 1_600_000_000_000L;
    when(rs.getTimestamp(eq("created_at"), any(Calendar.class))).thenReturn(new Timestamp(millis));

    DateTime result = handler.getResult(rs, "created_at");

    Assertions.assertEquals(millis, result.getMillis());
    ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
    verify(rs).getTimestamp(eq("created_at"), calendar.capture());
    Assertions.assertEquals(UTC, calendar.getValue().getTimeZone());
  }

  @Test
  public void should_return_null_when_column_name_result_is_null() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(anyString(), any(Calendar.class))).thenReturn(null);

    Assertions.assertNull(handler.getResult(rs, "created_at"));
  }

  @Test
  public void should_read_result_by_column_index() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    long millis = 1_600_000_000_000L;
    when(rs.getTimestamp(eq(3), any(Calendar.class))).thenReturn(new Timestamp(millis));

    DateTime result = handler.getResult(rs, 3);

    Assertions.assertEquals(millis, result.getMillis());
    ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
    verify(rs).getTimestamp(eq(3), calendar.capture());
    Assertions.assertEquals(UTC, calendar.getValue().getTimeZone());
  }

  @Test
  public void should_return_null_when_column_index_result_is_null() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(null);

    Assertions.assertNull(handler.getResult(rs, 3));
  }

  @Test
  public void should_read_result_from_callable_statement() throws Exception {
    CallableStatement cs = mock(CallableStatement.class);
    long millis = 1_600_000_000_000L;
    when(cs.getTimestamp(eq(1), any(Calendar.class))).thenReturn(new Timestamp(millis));

    DateTime result = handler.getResult(cs, 1);

    Assertions.assertEquals(millis, result.getMillis());
    ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
    verify(cs).getTimestamp(eq(1), calendar.capture());
    Assertions.assertEquals(UTC, calendar.getValue().getTimeZone());
  }

  @Test
  public void should_return_null_when_callable_statement_result_is_null() throws Exception {
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(null);

    Assertions.assertNull(handler.getResult(cs, 1));
  }
}
