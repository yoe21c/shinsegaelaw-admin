package com.shinsegaelaw.admin.repository;

import com.shinsegaelaw.admin.model.entity.Counsel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselRepository extends JpaRepository<Counsel, Long> {

    Optional<Counsel> findByCustomerPhoneNumber(String phoneNumber);

    List<Counsel> findByStatusOrderBySeq(String status);

    List<Counsel> findByCustomerPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}