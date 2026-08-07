package io.spring.infrastructure.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

@MappedTypes(Instant.class)
public class DateTimeHandler implements TypeHandler<Instant> {

  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  /**
   * JDBC drivers mutate the supplied {@link Calendar} while converting timestamps, so each call
   * gets its own instance rather than sharing one across request threads.
   */
  private static Calendar utcCalendar() {
    return Calendar.getInstance(UTC);
  }

  @Override
  public void setParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setTimestamp(
        i, parameter != null ? new Timestamp(parameter.toEpochMilli()) : null, utcCalendar());
  }

  @Override
  public Instant getResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName, utcCalendar());
    return timestamp != null ? Instant.ofEpochMilli(timestamp.getTime()) : null;
  }

  @Override
  public Instant getResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnIndex, utcCalendar());
    return timestamp != null ? Instant.ofEpochMilli(timestamp.getTime()) : null;
  }

  @Override
  public Instant getResult(CallableStatement cs, int columnIndex) throws SQLException {
    Timestamp ts = cs.getTimestamp(columnIndex, utcCalendar());
    return ts != null ? Instant.ofEpochMilli(ts.getTime()) : null;
  }
}
