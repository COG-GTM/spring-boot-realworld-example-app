package io.spring.infrastructure.mybatis.readservice;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MyBatisTagReadServiceMapper {
  List<String> all();
}
