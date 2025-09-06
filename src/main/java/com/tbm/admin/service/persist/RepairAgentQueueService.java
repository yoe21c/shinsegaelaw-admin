package com.tbm.admin.service.persist;

import com.tbm.admin.model.entity.RepairAgentQueue;
import com.tbm.admin.repository.RepairAgentQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairAgentQueueService {
    private final RepairAgentQueueRepository repairAgentQueueRepository;

    public Page<RepairAgentQueue> getRepairAgentQueues(Pageable pageable, String displayStatus, String keyword) {
        final List<String> status;
        if(displayStatus.equals("all")) {
            status = List.of("reserved", "processing", "completed", "failed"); // deleted 만 제외된.
        }else {
            status = List.of(displayStatus);
        }
        return repairAgentQueueRepository.findAllByStatusInOrderBySeqDesc(status, keyword, pageable);
    }

    public List<RepairAgentQueue> getAllByStatusInTop10(Long agentSeq) {
        final List<String> status = List.of("reserved", "processing", "completed", "failed"); // deleted 만 제외된.
        return repairAgentQueueRepository.findAllByStatusInTop10(agentSeq, status);
    }

    // 해야될것과 하고 있는 것들을 전부 교체한다.
    public List<RepairAgentQueue> findAllToUpsert() {
        final List<String> status = List.of("reserved", "processing");
        return repairAgentQueueRepository.findAllToUpsert(status);
    }

    public RepairAgentQueue getRepairAgentQueue(Long seq) {
        return repairAgentQueueRepository.findById(seq).orElseThrow(() -> new RuntimeException("RepairAgentQueue not found"));
    }

    public void save(RepairAgentQueue repairAgentQueue) {
        repairAgentQueueRepository.save(repairAgentQueue);
    }

    public List<RepairAgentQueue> candidateRepairAgents() {
        return repairAgentQueueRepository.findAllCandidates("reserved", LocalDateTime.now());
    }

    public void delete(Long seq) {
        repairAgentQueueRepository.deleteById(seq);
    }

    public void deleteAll(List<RepairAgentQueue> allRepairAgentQueues) {

        repairAgentQueueRepository.deleteAll(allRepairAgentQueues);
    }

    public void saveAll(List<RepairAgentQueue> newRepairAgentQueues) {

        repairAgentQueueRepository.saveAll(newRepairAgentQueues);
    }
}