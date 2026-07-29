package io.spring.api;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.lp.LPCommandService;
import io.spring.application.lp.LPQueryService;
import io.spring.application.lp.NewActivityParam;
import io.spring.application.lp.UpdateLPParam;
import io.spring.application.lp.UpdateStageParam;
import io.spring.core.lp.Activity;
import io.spring.core.lp.LP;
import io.spring.core.lp.LPRepository;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/lps/{id}")
@AllArgsConstructor
public class LPApi {
  private LPQueryService lpQueryService;
  private LPRepository lpRepository;
  private LPCommandService lpCommandService;

  @GetMapping
  public ResponseEntity<?> getLP(
      @PathVariable("id") String id, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(lpResponse(findOwnedLP(id, user)));
  }

  @PutMapping
  public ResponseEntity<?> updateLP(
      @PathVariable("id") String id,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody UpdateLPParam updateLPParam) {
    LP lp = lpCommandService.updateLP(findOwnedLP(id, user), updateLPParam);
    return ResponseEntity.ok(lpResponse(lp));
  }

  @DeleteMapping
  public ResponseEntity<?> deleteLP(
      @PathVariable("id") String id, @AuthenticationPrincipal User user) {
    lpRepository.remove(findOwnedLP(id, user));
    return ResponseEntity.noContent().build();
  }

  @PutMapping(path = "stage")
  public ResponseEntity<?> updateStage(
      @PathVariable("id") String id,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody UpdateStageParam updateStageParam) {
    LP lp = lpCommandService.updateStage(findOwnedLP(id, user), updateStageParam.getStage());
    return ResponseEntity.ok(lpResponse(lp));
  }

  @PostMapping(path = "activities")
  public ResponseEntity<?> createActivity(
      @PathVariable("id") String id,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody NewActivityParam newActivityParam) {
    Activity activity =
        lpCommandService.createActivity(findOwnedLP(id, user), newActivityParam, user);
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("activity", activity);
          }
        });
  }

  @GetMapping(path = "activities")
  public ResponseEntity<?> getActivities(
      @PathVariable("id") String id, @AuthenticationPrincipal User user) {
    List<Activity> activities = lpQueryService.findActivities(findOwnedLP(id, user));
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("activities", activities);
            put("activitiesCount", activities.size());
          }
        });
  }

  private LP findOwnedLP(String id, User user) {
    return lpQueryService.findById(id, user).orElseThrow(ResourceNotFoundException::new);
  }

  private Map<String, Object> lpResponse(LP lp) {
    return new HashMap<String, Object>() {
      {
        put("lp", lp);
      }
    };
  }
}
