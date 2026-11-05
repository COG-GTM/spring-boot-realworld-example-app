package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.golf.Golfer;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GolferMapper {
  void insert(@Param("golfer") Golfer golfer);

  void update(@Param("golfer") Golfer golfer);

  Golfer findById(@Param("id") String id);

  List<Golfer> findAll();
}
