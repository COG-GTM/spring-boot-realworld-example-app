package io.spring.application.lp;

import io.spring.core.lp.Activity;
import io.spring.core.lp.ActivityRepository;
import io.spring.core.lp.LP;
import io.spring.core.lp.LPRepository;
import io.spring.core.lp.Stage;
import io.spring.core.user.User;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class LPCommandService {

  private LPRepository lpRepository;
  private ActivityRepository activityRepository;

  public LP createLP(@Valid NewLPParam newLPParam, User creator) {
    LP lp =
        new LP(
            newLPParam.getName(),
            newLPParam.getCompany(),
            newLPParam.getEmail(),
            newLPParam.getStage(),
            creator.getId());
    lpRepository.save(lp);
    return lp;
  }

  public LP updateLP(LP lp, @Valid UpdateLPParam updateLPParam) {
    lp.update(updateLPParam.getName(), updateLPParam.getCompany(), updateLPParam.getEmail());
    lpRepository.save(lp);
    return lp;
  }

  public LP updateStage(LP lp, Stage stage) {
    lp.moveToStage(stage);
    lpRepository.save(lp);
    return lp;
  }

  public Activity createActivity(LP lp, @Valid NewActivityParam newActivityParam, User creator) {
    Activity activity =
        new Activity(
            lp.getId(), creator.getId(), newActivityParam.getType(), newActivityParam.getNotes());
    activityRepository.save(activity);
    return activity;
  }
}
