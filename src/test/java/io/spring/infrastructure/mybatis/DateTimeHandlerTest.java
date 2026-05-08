package io.spring.infrastructure.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DateTimeHandlerTest {

  private final DateTimeHandler handler = new DateTimeHandler();

  @Mock private PreparedStatement preparedStatement;
  @Mock private ResultSet resultSet;
  @Mock private CallableStatement callableStatement;

  @Test
  public void should_set_parameter_with_dateTime() throws Exception {
    DateTime dateTime = new DateTime(2024, 1, 15, 12, 0, DateTimeZone.UTC);

    handler.setParameter(preparedStatement, 1, dateTime, null);

    verify(preparedStatement, times(1)).setTimestamp(eq(1), any(Timestamp.class), any());
  }

  @Test
  public void should_set_parameter_with_null() throws Exception {
    handler.setParameter(preparedStatement, 1, null, null);

    verify(preparedStatement, times(1)).setTimestamp(eq(1), isNull(), any());
  }

  @Test
  public void should_get_result_by_column_name() throws Exception {
    DateTime dateTime = new DateTime(2024, 1, 15, 12, 0, DateTimeZone.UTC);
    when(resultSet.getTimestamp(eq("created_at"), any()))
        .thenReturn(new Timestamp(dateTime.getMillis()));

    DateTime result = handler.getResult(resultSet, "created_at");

    assertEquals(dateTime.getMillis(), result.getMillis());
  }

  @Test
  public void should_return_null_when_column_value_is_null_by_name() throws Exception {
    when(resultSet.getTimestamp(eq("created_at"), any())).thenReturn(null);

    assertNull(handler.getResult(resultSet, "created_at"));
  }

  @Test
  public void should_get_result_by_column_index() throws Exception {
    DateTime dateTime = new DateTime(2024, 1, 15, 12, 0, DateTimeZone.UTC);
    when(resultSet.getTimestamp(eq(1), any())).thenReturn(new Timestamp(dateTime.getMillis()));

    DateTime result = handler.getResult(resultSet, 1);

    assertEquals(dateTime.getMillis(), result.getMillis());
  }

  @Test
  public void should_return_null_when_column_value_is_null_by_index() throws Exception {
    when(resultSet.getTimestamp(eq(1), any())).thenReturn(null);

    assertNull(handler.getResult(resultSet, 1));
  }

  @Test
  public void should_get_result_from_callable_statement() throws Exception {
    DateTime dateTime = new DateTime(2024, 1, 15, 12, 0, DateTimeZone.UTC);
    when(callableStatement.getTimestamp(eq(1), any()))
        .thenReturn(new Timestamp(dateTime.getMillis()));

    DateTime result = handler.getResult(callableStatement, 1);

    assertEquals(dateTime.getMillis(), result.getMillis());
  }

  @Test
  public void should_return_null_from_callable_statement_when_value_is_null() throws Exception {
    when(callableStatement.getTimestamp(eq(1), any())).thenReturn(null);

    assertNull(handler.getResult(callableStatement, 1));
  }
}
