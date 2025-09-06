package com.tbm.admin.repository;

import com.tbm.admin.model.entity.SearchKeywordTeamSchedule;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SearchKeywordTeamScheduleRepository extends BaseRepository<SearchKeywordTeamSchedule, Long> {

    List<SearchKeywordTeamSchedule> findBySearchTimeOrderBySearchTimeAsc(LocalTime now);

    List<SearchKeywordTeamSchedule> findByTeamSeqOrderBySearchTimeAsc(Long teamSeq);

    Optional<SearchKeywordTeamSchedule> findByTeamSeqAndSearchTime(Long teamSeq, LocalTime searchTime);

    SearchKeywordTeamSchedule findBySeq(Long seq);

}