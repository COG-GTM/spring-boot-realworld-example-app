package io.spring.infrastructure.mybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class DateTimeHandlerTest {
  private static final DateTime NOW = new DateTime(2021, 5, 6, 7, 8, 9, DateTimeZone.UTC);

  private DateTimeHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new DateTimeHandler();
  }

  @Test
  public void should_set_timestamp_parameter_in_utc() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    handler.setParameter(ps, 1, NOW, null);

    ArgumentCaptor<Timestamp> timestampCaptor = ArgumentCaptor.forClass(Timestamp.class);
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
    verify(ps).setTimestamp(eq(1), timestampCaptor.capture(), calendarCaptor.capture());
    assertThat(timestampCaptor.getValue().getTime()).isEqualTo(NOW.getMillis());
    assertThat(calendarCaptor.getValue().getTimeZone()).isEqualTo(TimeZone.getTimeZone("UTC"));
  }

  @Test
  public void should_set_null_parameter_when_date_time_is_null() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);

    handler.setParameter(ps, 2, null, null);

    verify(ps).setTimestamp(eq(2), isNull(), any(Calendar.class));
  }

  @Test
  public void should_get_result_from_result_set_by_column_name() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(anyString(), any(Calendar.class)))
        .thenReturn(new Timestamp(NOW.getMillis()));

    assertThat(handler.getResult(rs, "created_at").getMillis()).isEqualTo(NOW.getMillis());
  }

  @Test
  public void should_get_null_result_from_result_set_by_column_name() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(anyString(), any(Calendar.class))).thenReturn(null);

    assertThat(handler.getResult(rs, "created_at")).isNull();
  }

  @Test
  public void should_get_result_from_result_set_by_column_index() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(new Timestamp(NOW.getMillis()));

    assertThat(handler.getResult(rs, 3).getMillis()).isEqualTo(NOW.getMillis());
  }

  @Test
  public void should_get_null_result_from_result_set_by_column_index() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(null);

    assertThat(handler.getResult(rs, 3)).isNull();
  }

  @Test
  public void should_get_result_from_callable_statement() throws SQLException {
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(new Timestamp(NOW.getMillis()));

    assertThat(handler.getResult(cs, 1).getMillis()).isEqualTo(NOW.getMillis());
  }

  @Test
  public void should_get_null_result_from_callable_statement() throws SQLException {
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(null);

    assertThat(handler.getResult(cs, 1)).isNull();
  }
}
