package com.shinsegaelaw.admin.repository;

import com.shinsegaelaw.admin.model.entity.AdminMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminMemberRepository extends BaseRepository<AdminMember, Long> {

    Page<AdminMember> findAll(Pageable pageable);

    AdminMember findByEmail(String email);

    AdminMember findBySeq(Long seq);

    @Query("SELECT DISTINCT a.depart FROM AdminMember a WHERE a.depart IS NOT NULL AND a.depart != '' ORDER BY a.depart")
    List<String> findDistinctDepartments();

}