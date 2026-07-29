package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.lp.LP;
import io.spring.core.lp.Relationship;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LPMapper {
  void insert(@Param("lp") LP lp);

  LP findById(@Param("id") String id);

  List<LP> findByUserId(@Param("userId") String userId);

  void update(@Param("lp") LP lp);

  void delete(@Param("id") String id);

  void insertRelationship(@Param("relationship") Relationship relationship);

  Relationship findRelationship(@Param("lpId") String lpId, @Param("contactId") String contactId);

  List<Relationship> findRelationships(@Param("lpId") String lpId);

  void deleteRelationship(@Param("relationship") Relationship relationship);
}
