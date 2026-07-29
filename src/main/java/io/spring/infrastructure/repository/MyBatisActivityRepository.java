package io.spring.infrastructure.repository;

import io.spring.core.lp.Activity;
import io.spring.core.lp.ActivityRepository;
import io.spring.infrastructure.mybatis.mapper.ActivityMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisActivityRepository implements ActivityRepository {
  private final ActivityMapper activityMapper;

  public MyBatisActivityRepository(ActivityMapper activityMapper) {
    this.activityMapper = activityMapper;
  }

  @Override
  public void save(Activity activity) {
    if (activityMapper.findById(activity.getId()) == null) {
      activityMapper.insert(activity);
    }
  }

  @Override
  public Optional<Activity> findById(String id) {
    return Optional.ofNullable(activityMapper.findById(id));
  }

  @Override
  public List<Activity> findByLpId(String lpId) {
    return activityMapper.findByLpId(lpId);
  }

  @Override
  public void remove(Activity activity) {
    activityMapper.delete(activity.getId());
  }
}
