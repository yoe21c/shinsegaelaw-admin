package com.shinsegaelaw.admin.service.persist;

import com.shinsegaelaw.admin.model.entity.AdminMemberHistory;
import com.shinsegaelaw.admin.repository.AdminMemberHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberHistoryService {

    private final AdminMemberHistoryRepository adminMemberHistoryRepository;

    // 로그인, 로그아웃을 빼고 .
    public Page<AdminMemberHistory> getAllWithoutLoginLogOut(String keyword, Pageable pageable) {
        return adminMemberHistoryRepository.findAllWithoutLoginLogOut(keyword, pageable);
    }

    // 전체
    public Page<AdminMemberHistory> getAllWith(String keyword, Pageable pageable) {
        return adminMemberHistoryRepository.findAllWith(keyword, pageable);
    }

    // 전체
    public List<AdminMemberHistory> getAllMemberHistories(Long memberSeq, List<String> actions) {
        return adminMemberHistoryRepository.findAllByMemberSeqAndActionNotInOrderBySeqDesc(memberSeq, actions);
    }

    // 전체
    public List<AdminMemberHistory> getAllMembers(List<Long> memberSeq) {
        return adminMemberHistoryRepository.findAllByMemberSeqIn(memberSeq);
    }

    public AdminMemberHistory save(AdminMemberHistory adminMemberHistory) {
        return adminMemberHistoryRepository.save(adminMemberHistory);
    }
}