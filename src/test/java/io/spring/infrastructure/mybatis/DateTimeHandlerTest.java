package io.spring.infrastructure.mybatis;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;

public class DateTimeHandlerTest {

  @Test
  public void should_set_timestamp_with_utc_calendar() throws Exception {
    PreparedStatement statement = mock(PreparedStatement.class);
    DateTime value = new DateTime(12345);
    DateTimeHandler handler = new DateTimeHandler();
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);

    handler.setParameter(statement, 1, value, JdbcType.TIMESTAMP);

    verify(statement)
        .setTimestamp(eq(1), eq(new Timestamp(value.getMillis())), calendarCaptor.capture());
    assertThat(calendarCaptor.getValue().getTimeZone().getID(), is("UTC"));
  }

  @Test
  public void should_set_null_timestamp() throws Exception {
    PreparedStatement statement = mock(PreparedStatement.class);
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);

    new DateTimeHandler().setParameter(statement, 1, null, JdbcType.TIMESTAMP);

    verify(statement)
        .setTimestamp(
            eq(1), org.mockito.ArgumentMatchers.<Timestamp>isNull(), calendarCaptor.capture());
    assertThat(calendarCaptor.getValue().getTimeZone().getID(), is("UTC"));
  }

  @Test
  public void should_read_timestamp_by_column_name() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
    when(resultSet.getTimestamp(eq("col"), any(Calendar.class))).thenReturn(new Timestamp(12345));

    DateTime result = new DateTimeHandler().getResult(resultSet, "col");

    assertThat(result.getMillis(), is(12345L));
    verify(resultSet).getTimestamp(eq("col"), calendarCaptor.capture());
    assertThat(calendarCaptor.getValue().getTimeZone().getID(), is("UTC"));
  }

  @Test
  public void should_read_null_timestamp_by_column_name() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
    when(resultSet.getTimestamp(eq("col"), any(Calendar.class))).thenReturn(null);

    assertThat(new DateTimeHandler().getResult(resultSet, "col"), is((DateTime) null));
    verify(resultSet).getTimestamp(eq("col"), calendarCaptor.capture());
    assertThat(calendarCaptor.getValue().getTimeZone().getID(), is("UTC"));
  }

  @Test
  public void should_read_timestamp_by_column_index() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
    when(resultSet.getTimestamp(eq(1), any(Calendar.class))).thenReturn(new Timestamp(12345));

    DateTime result = new DateTimeHandler().getResult(resultSet, 1);

    assertThat(result.getMillis(), is(12345L));
    verify(resultSet).getTimestamp(eq(1), calendarCaptor.capture());
    assertThat(calendarCaptor.getValue().getTimeZone().getID(), is("UTC"));
  }

  @Test
  public void should_read_null_timestamp_by_column_index() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
    when(resultSet.getTimestamp(eq(1), any(Calendar.class))).thenReturn(null);

    assertThat(new DateTimeHandler().getResult(resultSet, 1), is((DateTime) null));
    verify(resultSet).getTimestamp(eq(1), calendarCaptor.capture());
    assertThat(calendarCaptor.getValue().getTimeZone().getID(), is("UTC"));
  }

  @Test
  public void should_read_timestamp_from_callable_statement() throws Exception {
    CallableStatement statement = mock(CallableStatement.class);
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
    when(statement.getTimestamp(eq(1), any(Calendar.class))).thenReturn(new Timestamp(12345));

    DateTime result = new DateTimeHandler().getResult(statement, 1);

    assertThat(result.getMillis(), is(12345L));
    verify(statement).getTimestamp(eq(1), calendarCaptor.capture());
    assertThat(calendarCaptor.getValue().getTimeZone().getID(), is("UTC"));
  }

  @Test
  public void should_read_null_timestamp_from_callable_statement() throws Exception {
    CallableStatement statement = mock(CallableStatement.class);
    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
    when(statement.getTimestamp(eq(1), any(Calendar.class))).thenReturn(null);

    assertThat(new DateTimeHandler().getResult(statement, 1), is((DateTime) null));
    verify(statement).getTimestamp(eq(1), calendarCaptor.capture());
    assertThat(calendarCaptor.getValue().getTimeZone().getID(), is("UTC"));
  }
}
