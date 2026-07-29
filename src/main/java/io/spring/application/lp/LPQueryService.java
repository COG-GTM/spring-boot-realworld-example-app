package io.spring.application.lp;

import io.spring.core.lp.Activity;
import io.spring.core.lp.ActivityRepository;
import io.spring.core.lp.LP;
import io.spring.core.lp.LPRepository;
import io.spring.core.lp.Stage;
import io.spring.core.user.User;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LPQueryService {
  private LPRepository lpRepository;
  private ActivityRepository activityRepository;

  public Optional<LP> findById(String id, User user) {
    return lpRepository.findById(id).filter(lp -> lp.getUserId().equals(user.getId()));
  }

  public List<LP> findUserLPs(User user, Stage stage) {
    List<LP> lps = lpRepository.findByUserId(user.getId());
    if (stage == null) {
      return lps;
    }
    return lps.stream().filter(lp -> lp.getStage() == stage).collect(Collectors.toList());
  }

  public List<Activity> findActivities(LP lp) {
    return activityRepository.findByLpId(lp.getId());
  }
}
