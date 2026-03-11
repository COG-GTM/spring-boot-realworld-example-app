package io.spring.infrastructure.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

@MappedTypes(OffsetDateTime.class)
public class DateTimeHandler implements TypeHandler<OffsetDateTime> {

  private static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  @Override
  public void setParameter(PreparedStatement ps, int i, OffsetDateTime parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setTimestamp(
        i, parameter != null ? Timestamp.from(parameter.toInstant()) : null, UTC_CALENDAR);
  }

  @Override
  public OffsetDateTime getResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName, UTC_CALENDAR);
    return timestamp != null
        ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime()), ZoneOffset.UTC)
        : null;
  }

  @Override
  public OffsetDateTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnIndex, UTC_CALENDAR);
    return timestamp != null
        ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime()), ZoneOffset.UTC)
        : null;
  }

  @Override
  public OffsetDateTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    Timestamp ts = cs.getTimestamp(columnIndex, UTC_CALENDAR);
    return ts != null
        ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(ts.getTime()), ZoneOffset.UTC)
        : null;
  }
}
