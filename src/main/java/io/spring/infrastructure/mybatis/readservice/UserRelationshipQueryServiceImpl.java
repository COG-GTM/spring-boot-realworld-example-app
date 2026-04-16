package io.spring.infrastructure.mybatis.readservice;

import io.spring.core.user.UserRelationshipQueryService;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UserRelationshipQueryServiceImpl implements UserRelationshipQueryService {
  private final MyBatisUserRelationshipQueryServiceMapper mapper;

  public UserRelationshipQueryServiceImpl(MyBatisUserRelationshipQueryServiceMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean isUserFollowing(String userId, String anotherUserId) {
    return mapper.isUserFollowing(userId, anotherUserId);
  }

  @Override
  public Set<String> followingAuthors(String userId, List<String> ids) {
    return mapper.followingAuthors(userId, ids);
  }

  @Override
  public List<String> followedUsers(String userId) {
    return mapper.followedUsers(userId);
  }
}
