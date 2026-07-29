package io.spring.infrastructure.repository;

import io.spring.core.lp.LP;
import io.spring.core.lp.LPRepository;
import io.spring.core.lp.Relationship;
import io.spring.infrastructure.mybatis.mapper.LPMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MyBatisLPRepository implements LPRepository {
  private final LPMapper lpMapper;

  public MyBatisLPRepository(LPMapper lpMapper) {
    this.lpMapper = lpMapper;
  }

  @Override
  @Transactional
  public void save(LP lp) {
    if (lpMapper.findById(lp.getId()) == null) {
      lpMapper.insert(lp);
    } else {
      lpMapper.update(lp);
    }
  }

  @Override
  public Optional<LP> findById(String id) {
    return Optional.ofNullable(lpMapper.findById(id));
  }

  @Override
  public List<LP> findByUserId(String userId) {
    return lpMapper.findByUserId(userId);
  }

  @Override
  public void remove(LP lp) {
    lpMapper.delete(lp.getId());
  }

  @Override
  public void saveRelationship(Relationship relationship) {
    if (lpMapper.findRelationship(relationship.getLpId(), relationship.getContactId()) == null) {
      lpMapper.insertRelationship(relationship);
    }
  }

  @Override
  public List<Relationship> findRelationships(String lpId) {
    return lpMapper.findRelationships(lpId);
  }

  @Override
  public void removeRelationship(Relationship relationship) {
    lpMapper.deleteRelationship(relationship);
  }
}
