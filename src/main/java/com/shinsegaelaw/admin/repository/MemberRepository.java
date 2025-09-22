package com.shinsegaelaw.admin.repository;

import com.shinsegaelaw.admin.model.entity.AdminMember;

import java.util.Optional;

public interface MemberRepository extends BaseRepository<AdminMember, Long> {

    Optional<AdminMember> findByEmail(String email);

    AdminMember findBySeq(Long seq);
}