package io.spring.infrastructure.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

@MappedTypes(Instant.class)
public class DateTimeHandler implements TypeHandler<Instant> {

  @Override
  public void setParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setTimestamp(i, parameter != null ? Timestamp.from(parameter) : null);
  }

  @Override
  public Instant getResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName);
    return timestamp != null ? timestamp.toInstant() : null;
  }

  @Override
  public Instant getResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnIndex);
    return timestamp != null ? timestamp.toInstant() : null;
  }

  @Override
  public Instant getResult(CallableStatement cs, int columnIndex) throws SQLException {
    Timestamp ts = cs.getTimestamp(columnIndex);
    return ts != null ? ts.toInstant() : null;
  }
}
