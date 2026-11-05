package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.golf.Pick;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PickMapper {
  void insert(@Param("pick") Pick pick);

  void update(@Param("pick") Pick pick);

  Pick findById(@Param("id") String id);

  List<Pick> findByContestAndUser(
      @Param("contestId") String contestId, @Param("userId") String userId);

  Pick findByContestUserAndTournament(
      @Param("contestId") String contestId,
      @Param("userId") String userId,
      @Param("tournamentId") String tournamentId);

  int countByContestUserAndGolfer(
      @Param("contestId") String contestId,
      @Param("userId") String userId,
      @Param("golferId") String golferId);

  List<Pick> findByContest(@Param("contestId") String contestId);
}
