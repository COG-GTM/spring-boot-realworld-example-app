package io.spring.infrastructure.mybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
  public void should_set_timestamp_parameter() throws Exception {
    DateTime dateTime = new DateTime(1000000L);

    handler.setParameter(preparedStatement, 1, dateTime, null);

    verify(preparedStatement).setTimestamp(eq(1), eq(new Timestamp(1000000L)), any(Calendar.class));
  }

  @Test
  public void should_set_null_parameter_when_value_is_null() throws Exception {
    handler.setParameter(preparedStatement, 2, null, null);

    verify(preparedStatement).setTimestamp(eq(2), isNull(), any(Calendar.class));
  }

  @Test
  public void should_read_result_by_column_name() throws Exception {
    when(resultSet.getTimestamp(anyString(), any(Calendar.class)))
        .thenReturn(new Timestamp(1000000L));

    assertThat(handler.getResult(resultSet, "created_at")).isEqualTo(new DateTime(1000000L));
  }

  @Test
  public void should_return_null_for_null_column_name_result() throws Exception {
    when(resultSet.getTimestamp(anyString(), any(Calendar.class))).thenReturn(null);

    assertThat(handler.getResult(resultSet, "created_at")).isNull();
  }

  @Test
  public void should_read_result_by_column_index() throws Exception {
    when(resultSet.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(new Timestamp(2000000L));

    assertThat(handler.getResult(resultSet, 3)).isEqualTo(new DateTime(2000000L));
  }

  @Test
  public void should_return_null_for_null_column_index_result() throws Exception {
    when(resultSet.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(null);

    assertThat(handler.getResult(resultSet, 3)).isNull();
  }

  @Test
  public void should_read_result_from_callable_statement() throws Exception {
    when(callableStatement.getTimestamp(anyInt(), any(Calendar.class)))
        .thenReturn(new Timestamp(3000000L));

    assertThat(handler.getResult(callableStatement, 1)).isEqualTo(new DateTime(3000000L));
  }

  @Test
  public void should_return_null_for_null_callable_statement_result() throws Exception {
    when(callableStatement.getTimestamp(anyInt(), any(Calendar.class))).thenReturn(null);

    assertThat(handler.getResult(callableStatement, 1)).isNull();
  }
}
