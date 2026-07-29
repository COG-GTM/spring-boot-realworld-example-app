package io.spring.api;

import io.spring.application.lp.LPCommandService;
import io.spring.application.lp.LPQueryService;
import io.spring.application.lp.NewLPParam;
import io.spring.core.lp.LP;
import io.spring.core.lp.Stage;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.List;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/lps")
@AllArgsConstructor
public class LPsApi {
  private LPCommandService lpCommandService;
  private LPQueryService lpQueryService;

  @PostMapping
  public ResponseEntity<?> createLP(
      @Valid @RequestBody NewLPParam newLPParam, @AuthenticationPrincipal User user) {
    LP lp = lpCommandService.createLP(newLPParam, user);
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("lp", lp);
          }
        });
  }

  @GetMapping
  public ResponseEntity<?> getLPs(
      @RequestParam(value = "stage", required = false) Stage stage,
      @AuthenticationPrincipal User user) {
    List<LP> lps = lpQueryService.findUserLPs(user, stage);
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("lps", lps);
            put("lpsCount", lps.size());
          }
        });
  }
}
