package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.data.GolfContestData;
import io.spring.application.data.GolfPickData;
import io.spring.application.data.GolfTournamentData;
import io.spring.application.data.GolferData;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GolfReadService {
  List<GolfContestData> allContests();

  GolfContestData findContestById(@Param("id") String id);

  List<GolfTournamentData> findTournamentsByContest(@Param("contestId") String contestId);

  List<GolferData> allGolfers();

  boolean isEntered(@Param("contestId") String contestId, @Param("userId") String userId);

  List<GolfPickData> findPicksByContestAndUser(
      @Param("contestId") String contestId, @Param("userId") String userId);

  GolfPickData findPickById(@Param("id") String id);
}
