
package com.tbm.admin.repository;

import com.tbm.admin.model.entity.RepairCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepairCodeRepository extends JpaRepository<RepairCode, Long> {

    Page<RepairCode> findAllBySeqIsNotNullOrderBySeqDesc(Pageable pageable);

    Optional<RepairCode> findByName(String name);
}