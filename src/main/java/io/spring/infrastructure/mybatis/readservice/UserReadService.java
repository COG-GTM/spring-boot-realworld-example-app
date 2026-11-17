package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.data.UserData;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserReadService {

  UserData findByUsername(@Param("username") String username);

  List<UserData> findByUsernames(@Param("usernames") Collection<String> usernames);

  UserData findById(@Param("id") String id);
}
