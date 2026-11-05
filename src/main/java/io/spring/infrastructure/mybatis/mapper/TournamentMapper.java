package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.golf.Tournament;
import io.spring.core.golf.TournamentResult;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TournamentMapper {
  void insert(@Param("tournament") Tournament tournament);

  void update(@Param("tournament") Tournament tournament);

  Tournament findById(@Param("id") String id);

  List<Tournament> findByContestId(@Param("contestId") String contestId);

  void insertResult(@Param("result") TournamentResult result);

  void updateResult(@Param("result") TournamentResult result);

  TournamentResult findResult(
      @Param("tournamentId") String tournamentId, @Param("golferId") String golferId);

  List<TournamentResult> findResultsByTournamentId(@Param("tournamentId") String tournamentId);
}
