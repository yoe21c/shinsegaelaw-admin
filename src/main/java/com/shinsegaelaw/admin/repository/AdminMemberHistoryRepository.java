package com.shinsegaelaw.admin.repository;

import com.shinsegaelaw.admin.model.entity.AdminMemberHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminMemberHistoryRepository extends BaseRepository<AdminMemberHistory, Long> {

    @Query(value = """
        select * from AdminMemberHistory
        where seq is not null
         and action not in ('login', 'logout')
    """, countProjection = "seq", nativeQuery = true)
    Page<AdminMemberHistory> findAllWithoutLoginLogOut(String keyword, Pageable pageable);

    @Query(value = """
        select * from AdminMemberHistory
        where seq is not null
    """, countProjection = "seq", nativeQuery = true)
    Page<AdminMemberHistory> findAllWith(String keyword, Pageable pageable);

    List<AdminMemberHistory> findAllByMemberSeqAndActionNotInOrderBySeqDesc(Long memberSeq, List<String> actions);

    List<AdminMemberHistory> findAllByMemberSeqIn(List<Long> memberSequences);
}