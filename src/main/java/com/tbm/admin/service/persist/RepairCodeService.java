package com.tbm.admin.service.persist;

import com.tbm.admin.model.entity.RepairCode;
import com.tbm.admin.repository.RepairCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairCodeService {
    private final RepairCodeRepository repairCodeRepository;

    public Page<RepairCode> getRepairCodes(Pageable pageable) {
        return repairCodeRepository.findAllBySeqIsNotNullOrderBySeqDesc(pageable);
    }

    public RepairCode getRepairCode(Long seq) {
        return repairCodeRepository.findById(seq).orElseThrow(() -> new RuntimeException("RepairCode not found. seq: " + seq));
    }

    public RepairCode getRepairCodeByName(String name) {
        return repairCodeRepository.findByName(name).orElseThrow(() -> new RuntimeException("RepairCode not found. name: " + name));
    }

    public void save(RepairCode repairAgent) {
        repairCodeRepository.save(repairAgent);
    }

}