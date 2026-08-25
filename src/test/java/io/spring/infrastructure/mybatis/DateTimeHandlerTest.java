package io.spring.infrastructure.mybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import org.joda.time.DateTime;
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
  public void should_set_timestamp_parameter_from_datetime() throws SQLException {
    DateTime dateTime = new DateTime(1000L);

    handler.setParameter(preparedStatement, 1, dateTime, null);

    verify(preparedStatement).setTimestamp(eq(1), eq(new Timestamp(1000L)), any(Calendar.class));
  }

  @Test
  public void should_set_null_timestamp_parameter_for_null_datetime() throws SQLException {
    handler.setParameter(preparedStatement, 2, null, null);

    verify(preparedStatement).setTimestamp(eq(2), eq(null), any(Calendar.class));
  }

  @Test
  public void should_read_datetime_from_result_set_by_column_name() throws SQLException {
    when(resultSet.getTimestamp(anyString(), any(Calendar.class))).thenReturn(new Timestamp(1000L));

    assertThat(handler.getResult(resultSet, "created_at")).isEqualTo(new DateTime(1000L));
  }

  @Test
  public void should_read_datetime_from_result_set_by_column_index() throws SQLException {
    when(resultSet.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(new Timestamp(2000L));

    assertThat(handler.getResult(resultSet, 1)).isEqualTo(new DateTime(2000L));
  }

  @Test
  public void should_read_datetime_from_callable_statement() throws SQLException {
    when(callableStatement.getTimestamp(anyInt(), any(Calendar.class)))
        .thenReturn(new Timestamp(3000L));

    assertThat(handler.getResult(callableStatement, 1)).isEqualTo(new DateTime(3000L));
  }

  @Test
  public void should_return_null_when_stored_timestamp_is_null() throws SQLException {
    when(resultSet.getTimestamp(anyString(), any(Calendar.class))).thenReturn(null);
    when(resultSet.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(null);
    when(callableStatement.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(null);

    assertThat(handler.getResult(resultSet, "created_at")).isNull();
    assertThat(handler.getResult(resultSet, 1)).isNull();
    assertThat(handler.getResult(callableStatement, 1)).isNull();
  }
}
