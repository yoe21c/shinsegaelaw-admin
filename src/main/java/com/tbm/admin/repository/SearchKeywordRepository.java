package com.tbm.admin.repository;

import com.tbm.admin.model.entity.SearchKeyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchKeywordRepository extends BaseRepository<SearchKeyword, Long> {

    List<SearchKeyword> findByTeamSeq(Long teamSeq);

    Page<SearchKeyword> findByTeamSeq(Long teamSeq, Pageable pageable);

    SearchKeyword findBySeq(Long seq);

    SearchKeyword findByTeamSeqAndKeyword(Long seq, String keyword);

     // 상태별 필터링을 위한 새로운 메서드
     Page<SearchKeyword> findByTeamSeqAndStatus(Long teamSeq, String status, Pageable pageable);

    long countByTeamSeq(Long teamSeq);
    long countByTeamSeqAndStatus(Long teamSeq, String status);

}
