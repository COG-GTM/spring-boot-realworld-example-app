package io.spring.infrastructure.lp;

import io.spring.core.lp.Activity;
import io.spring.core.lp.ActivityRepository;
import io.spring.core.lp.LP;
import io.spring.core.lp.LPRepository;
import io.spring.core.lp.Relationship;
import io.spring.core.lp.Stage;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisActivityRepository;
import io.spring.infrastructure.repository.MyBatisLPRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisLPRepository.class, MyBatisActivityRepository.class, MyBatisUserRepository.class})
public class MyBatisLPRepositoryTest extends DbTestBase {
  @Autowired private LPRepository lpRepository;

  @Autowired private ActivityRepository activityRepository;

  @Autowired private UserRepository userRepository;

  private User user;
  private LP lp;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@gmail.com", "aisensiy", "123", "bio", "default");
    userRepository.save(user);
    lp = new LP("John Doe", "ACME", "john@acme.com", user.getId());
  }

  @Test
  public void should_create_and_fetch_lp_success() {
    lpRepository.save(lp);

    Optional<LP> optional = lpRepository.findById(lp.getId());
    Assertions.assertTrue(optional.isPresent());
    LP fetched = optional.get();
    Assertions.assertEquals(lp, fetched);
    Assertions.assertEquals("John Doe", fetched.getName());
    Assertions.assertEquals("ACME", fetched.getCompany());
    Assertions.assertEquals("john@acme.com", fetched.getEmail());
    Assertions.assertEquals(Stage.TERRITORY, fetched.getStage());
    Assertions.assertEquals(user.getId(), fetched.getUserId());
  }

  @Test
  public void should_update_and_fetch_lp_success() {
    lpRepository.save(lp);

    lp.update("Jane Doe", "", "jane@acme.com");
    lpRepository.save(lp);

    LP fetched = lpRepository.findById(lp.getId()).get();
    Assertions.assertEquals("Jane Doe", fetched.getName());
    Assertions.assertEquals("ACME", fetched.getCompany());
    Assertions.assertEquals("jane@acme.com", fetched.getEmail());
  }

  @Test
  public void should_move_lp_to_another_stage() {
    lpRepository.save(lp);

    lp.moveToStage(Stage.NEGOTIATION);
    lpRepository.save(lp);

    Assertions.assertEquals(Stage.NEGOTIATION, lpRepository.findById(lp.getId()).get().getStage());
  }

  @Test
  public void should_fetch_lps_of_user() {
    lpRepository.save(lp);
    lpRepository.save(new LP("Other", "Other Inc", "other@other.com", "another-user"));

    List<LP> lps = lpRepository.findByUserId(user.getId());
    Assertions.assertEquals(1, lps.size());
    Assertions.assertEquals(lp, lps.get(0));
  }

  @Test
  public void should_delete_lp() {
    lpRepository.save(lp);

    lpRepository.remove(lp);
    Assertions.assertFalse(lpRepository.findById(lp.getId()).isPresent());
  }

  @Test
  public void should_save_and_remove_relationship() {
    lpRepository.save(lp);
    Relationship relationship = new Relationship(lp.getId(), "contact-id", "CTO");

    lpRepository.saveRelationship(relationship);
    lpRepository.saveRelationship(relationship);

    List<Relationship> relationships = lpRepository.findRelationships(lp.getId());
    Assertions.assertEquals(1, relationships.size());
    Assertions.assertEquals(relationship, relationships.get(0));

    lpRepository.removeRelationship(relationship);
    Assertions.assertTrue(lpRepository.findRelationships(lp.getId()).isEmpty());
  }

  @Test
  public void should_create_and_fetch_activities_of_lp() {
    lpRepository.save(lp);
    Activity activity = new Activity(lp.getId(), user.getId(), "CALL", "first call");
    activityRepository.save(activity);

    Optional<Activity> optional = activityRepository.findById(activity.getId());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals("CALL", optional.get().getType());
    Assertions.assertEquals("first call", optional.get().getNotes());

    List<Activity> activities = activityRepository.findByLpId(lp.getId());
    Assertions.assertEquals(1, activities.size());
    Assertions.assertEquals(activity, activities.get(0));
  }

  @Test
  public void should_delete_activity() {
    lpRepository.save(lp);
    Activity activity = new Activity(lp.getId(), user.getId(), "EMAIL", "sent pact");
    activityRepository.save(activity);

    activityRepository.remove(activity);
    Assertions.assertTrue(activityRepository.findByLpId(lp.getId()).isEmpty());
  }
}
