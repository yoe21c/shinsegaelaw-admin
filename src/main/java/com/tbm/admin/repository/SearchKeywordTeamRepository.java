package com.tbm.admin.repository;

import com.tbm.admin.model.entity.SearchKeywordTeam;

import java.util.List;

public interface SearchKeywordTeamRepository extends BaseRepository<SearchKeywordTeam, Long> {

    List<SearchKeywordTeam> findAllBy();

    SearchKeywordTeam findBySeq(Long seq);

    SearchKeywordTeam findByDepart(String depart);

    SearchKeywordTeam findByTeamName(String teamName);

    List<SearchKeywordTeam> findByActive(boolean active);

    List<SearchKeywordTeam> findByDepartIn(List<String> departs);

}
