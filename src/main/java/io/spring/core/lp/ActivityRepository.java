package io.spring.core.lp;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository {

  void save(Activity activity);

  Optional<Activity> findById(String id);

  List<Activity> findByLpId(String lpId);

  void remove(Activity activity);
}
