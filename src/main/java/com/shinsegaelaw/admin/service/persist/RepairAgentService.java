package com.shinsegaelaw.admin.service.persist;

import com.shinsegaelaw.admin.model.entity.RepairAgent;
import com.shinsegaelaw.admin.repository.RepairAgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairAgentService {
    private final RepairAgentRepository repairAgentRepository;

    public Page<RepairAgent> getRepairAgents(Pageable pageable) {
        return repairAgentRepository.findAllBySeqIsNotNullOrderBySeqDesc(pageable);
    }

    public RepairAgent getRepairAgent(Long seq) {
        return repairAgentRepository.findById(seq).orElseThrow(() -> new RuntimeException("RepairAgent not found"));
    }

    public RepairAgent getRepairAgent(String blogId) {
        return repairAgentRepository.findRepairAgentByBlogId(blogId).orElseThrow(() -> new RuntimeException("RepairAgent not found"));
    }

    public void save(RepairAgent repairAgent) {
        repairAgentRepository.save(repairAgent);
    }

    public Optional<RepairAgent> getRepairAgentByIpAddress(String ipAddress) {
        return repairAgentRepository.findRepairAgentByIpAddress(ipAddress);
    }

    public void delete(Long seq) {
        repairAgentRepository.deleteById(seq);
    }

}