package com.tbm.admin.service.persist;

import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public AdminMember getMemberBySeq(Long seq) {
        return memberRepository.findBySeq(seq);
    }
    public void save(AdminMember adminMember) {
        memberRepository.save(adminMember);
    }

    public List<AdminMember> getAllMembers() {
        return memberRepository.findAll();
    }
}
