
package com.tbm.admin.repository;

import com.tbm.admin.model.entity.RepairAgentQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RepairAgentQueueRepository extends JpaRepository<RepairAgentQueue, Long> {

    @Query(value = """
        select r
        from RepairAgentQueue r 
        where r.status in :status
            and (r.blogId like concat('%', :keyword, '%') 
                or r.ipAddress like concat('%', :keyword, '%')
                or r.title like concat('%', :keyword, '%')
            )
    """, nativeQuery = false)
    Page<RepairAgentQueue> findAllByStatusInOrderBySeqDesc(List<String> status, String keyword, Pageable pageable);

    @Query(value = """
        select r
        from RepairAgentQueue r 
        where r.agentSeq = :agentSeq and r.status in :status
        order by r.seq desc
        limit 10
    """, nativeQuery = false)
    List<RepairAgentQueue> findAllByStatusInTop10(Long agentSeq, List<String> status);

    @Query(value = """
        select r
        from RepairAgentQueue r 
        where r.status in :status
    """, nativeQuery = false)
    List<RepairAgentQueue> findAllToUpsert(List<String> status);

    @Query("""
        select r 
        from RepairAgentQueue r 
        where r.status = :status 
            and r.reservedAt < :reservedAt 
        order by r.seq
    """)
    List<RepairAgentQueue> findAllCandidates(String status, LocalDateTime reservedAt);
}