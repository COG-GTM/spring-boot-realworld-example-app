package io.spring.infrastructure.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

@MappedTypes(ZonedDateTime.class)
public class DateTimeHandler implements TypeHandler<ZonedDateTime> {

  private static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  @Override
  public void setParameter(PreparedStatement ps, int i, ZonedDateTime parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setTimestamp(
        i, parameter != null ? Timestamp.from(parameter.toInstant()) : null, UTC_CALENDAR);
  }

  @Override
  public ZonedDateTime getResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName, UTC_CALENDAR);
    return timestamp != null ? ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC) : null;
  }

  @Override
  public ZonedDateTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnIndex, UTC_CALENDAR);
    return timestamp != null ? ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC) : null;
  }

  @Override
  public ZonedDateTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    Timestamp ts = cs.getTimestamp(columnIndex, UTC_CALENDAR);
    return ts != null ? ZonedDateTime.ofInstant(ts.toInstant(), ZoneOffset.UTC) : null;
  }
}
