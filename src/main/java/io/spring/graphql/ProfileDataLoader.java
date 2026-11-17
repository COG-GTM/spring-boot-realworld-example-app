package io.spring.graphql;

import com.netflix.graphql.dgs.DgsDataLoader;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Profile;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import lombok.AllArgsConstructor;
import org.dataloader.MappedBatchLoader;

/** Batches profile lookups by username so a list of articles loads its authors in one pass. */
@DgsDataLoader(name = ProfileDataLoader.NAME)
@AllArgsConstructor
public class ProfileDataLoader implements MappedBatchLoader<String, Profile> {
  public static final String NAME = "profiles";

  private ProfileQueryService profileQueryService;

  @Override
  public CompletionStage<Map<String, Profile>> load(Set<String> usernames) {
    User current = SecurityUtil.getCurrentUser().orElse(null);
    Map<String, Profile> profiles = new HashMap<>();
    profileQueryService
        .findByUsernames(usernames, current)
        .forEach((username, profileData) -> profiles.put(username, toProfile(profileData)));
    return CompletableFuture.completedFuture(profiles);
  }

  static Profile toProfile(ProfileData profileData) {
    return Profile.newBuilder()
        .username(profileData.getUsername())
        .bio(profileData.getBio())
        .image(profileData.getImage())
        .following(profileData.isFollowing())
        .build();
  }
}
