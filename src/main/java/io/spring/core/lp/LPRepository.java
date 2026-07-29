package io.spring.core.lp;

import java.util.List;
import java.util.Optional;

public interface LPRepository {

  void save(LP lp);

  Optional<LP> findById(String id);

  List<LP> findByUserId(String userId);

  void remove(LP lp);

  void saveRelationship(Relationship relationship);

  List<Relationship> findRelationships(String lpId);

  void removeRelationship(Relationship relationship);
}
