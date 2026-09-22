package io.spring.infrastructure.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  @Mock private CallableStatement callableStatement;
  @Mock private ResultSet resultSet;

  @Test
  public void should_set_timestamp_parameter() throws Exception {
    handler.setParameter(preparedStatement, 1, new DateTime(1000L), null);

    verify(preparedStatement).setTimestamp(eq(1), eq(new Timestamp(1000L)), any(Calendar.class));
  }

  @Test
  public void should_set_null_parameter() throws Exception {
    handler.setParameter(preparedStatement, 1, null, null);

    verify(preparedStatement).setTimestamp(eq(1), eq(null), any(Calendar.class));
  }

  @Test
  public void should_read_result_by_column_name() throws Exception {
    when(resultSet.getTimestamp(eq("created_at"), any(Calendar.class)))
        .thenReturn(new Timestamp(1000L));

    assertEquals(1000L, handler.getResult(resultSet, "created_at").getMillis());
  }

  @Test
  public void should_read_result_by_column_index() throws Exception {
    when(resultSet.getTimestamp(eq(1), any(Calendar.class))).thenReturn(new Timestamp(2000L));

    assertEquals(2000L, handler.getResult(resultSet, 1).getMillis());
  }

  @Test
  public void should_read_result_from_callable_statement() throws Exception {
    when(callableStatement.getTimestamp(eq(1), any(Calendar.class)))
        .thenReturn(new Timestamp(3000L));

    assertEquals(3000L, handler.getResult(callableStatement, 1).getMillis());
  }

  @Test
  public void should_read_null_result() throws Exception {
    when(resultSet.getTimestamp(eq("created_at"), any(Calendar.class))).thenReturn(null);

    assertNull(handler.getResult(resultSet, "created_at"));
  }
}
