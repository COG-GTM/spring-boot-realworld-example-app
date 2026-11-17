package io.spring.application;

import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProfileQueryService {
  private UserReadService userReadService;
  private UserRelationshipQueryService userRelationshipQueryService;

  public Optional<ProfileData> findByUsername(String username, User currentUser) {
    UserData userData = userReadService.findByUsername(username);
    if (userData == null) {
      return Optional.empty();
    } else {
      ProfileData profileData =
          new ProfileData(
              userData.getId(),
              userData.getUsername(),
              userData.getBio(),
              userData.getImage(),
              currentUser != null
                  && userRelationshipQueryService.isUserFollowing(
                      currentUser.getId(), userData.getId()));
      return Optional.of(profileData);
    }
  }

  public Map<String, ProfileData> findByUsernames(Collection<String> usernames, User currentUser) {
    if (usernames.isEmpty()) {
      return Collections.emptyMap();
    }
    List<UserData> users = userReadService.findByUsernames(usernames);
    Set<String> following =
        currentUser == null || users.isEmpty()
            ? Collections.emptySet()
            : userRelationshipQueryService.followingAuthors(
                currentUser.getId(),
                users.stream().map(UserData::getId).collect(Collectors.toList()));
    Map<String, ProfileData> result = new HashMap<>();
    for (UserData userData : users) {
      result.put(
          userData.getUsername(),
          new ProfileData(
              userData.getId(),
              userData.getUsername(),
              userData.getBio(),
              userData.getImage(),
              following.contains(userData.getId())));
    }
    return result;
  }
}
