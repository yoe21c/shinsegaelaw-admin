package com.shinsegaelaw.admin.repository;

import com.shinsegaelaw.admin.model.entity.RepairAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepairAgentRepository extends JpaRepository<RepairAgent, Long> {

    Optional<RepairAgent> findRepairAgentByIpAddress(String ipAddress);

    Optional<RepairAgent> findRepairAgentByBlogId(String blogId);

    Page<RepairAgent> findAllBySeqIsNotNullOrderBySeqDesc(Pageable pageable);
}