package io.spring.infrastructure.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.apache.ibatis.type.JdbcType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class DateTimeHandlerTest {

  private DateTimeHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new DateTimeHandler();
  }

  @Test
  public void should_set_timestamp_parameter_with_utc_calendar() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);
    DateTime dateTime = new DateTime(1_600_000_000_000L);

    handler.setParameter(ps, 1, dateTime, JdbcType.TIMESTAMP);

    ArgumentCaptor<Timestamp> timestamp = ArgumentCaptor.forClass(Timestamp.class);
    ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
    verify(ps).setTimestamp(eq(1), timestamp.capture(), calendar.capture());
    assertEquals(dateTime.getMillis(), timestamp.getValue().getTime());
    assertEquals("UTC", calendar.getValue().getTimeZone().getID());
  }

  @Test
  public void should_set_null_timestamp_when_parameter_is_null() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);

    handler.setParameter(ps, 2, null, JdbcType.TIMESTAMP);

    verify(ps).setTimestamp(eq(2), isNull(), any(Calendar.class));
  }

  @Test
  public void should_get_result_by_column_name() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(eq("created_at"), any(Calendar.class)))
        .thenReturn(new Timestamp(1_600_000_000_000L));

    DateTime result = handler.getResult(rs, "created_at");

    assertEquals(1_600_000_000_000L, result.getMillis());
  }

  @Test
  public void should_get_result_by_column_index() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(eq(3), any(Calendar.class))).thenReturn(new Timestamp(1_600_000_000_000L));

    DateTime result = handler.getResult(rs, 3);

    assertEquals(1_600_000_000_000L, result.getMillis());
  }

  @Test
  public void should_get_result_from_callable_statement() throws Exception {
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getTimestamp(eq(1), any(Calendar.class))).thenReturn(new Timestamp(1_600_000_000_000L));

    DateTime result = handler.getResult(cs, 1);

    assertEquals(1_600_000_000_000L, result.getMillis());
  }

  @Test
  public void should_return_null_when_timestamp_is_null() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    CallableStatement cs = mock(CallableStatement.class);

    assertNull(handler.getResult(rs, "created_at"));
    assertNull(handler.getResult(rs, 1));
    assertNull(handler.getResult(cs, 1));
  }

  @Test
  public void should_use_utc_calendar_when_reading() throws Exception {
    ResultSet rs = mock(ResultSet.class);

    handler.getResult(rs, "created_at");
    handler.getResult(rs, 1);

    ArgumentCaptor<Calendar> byName = ArgumentCaptor.forClass(Calendar.class);
    ArgumentCaptor<Calendar> byIndex = ArgumentCaptor.forClass(Calendar.class);
    verify(rs).getTimestamp(eq("created_at"), byName.capture());
    verify(rs).getTimestamp(anyInt(), byIndex.capture());
    assertEquals("UTC", byName.getValue().getTimeZone().getID());
    assertEquals("UTC", byIndex.getValue().getTimeZone().getID());
  }

  @Test
  public void should_not_share_calendar_instance_between_calls() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);

    handler.setParameter(ps, 1, new DateTime(), JdbcType.TIMESTAMP);
    handler.setParameter(ps, 1, new DateTime(), JdbcType.TIMESTAMP);

    ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
    verify(ps, times(2)).setTimestamp(eq(1), any(Timestamp.class), calendar.capture());
    List<Calendar> calendars = calendar.getAllValues();
    assertNotSame(calendars.get(0), calendars.get(1));
  }
}
