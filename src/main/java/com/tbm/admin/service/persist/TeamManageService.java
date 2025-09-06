package com.tbm.admin.service.persist;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.Depart;
import com.tbm.admin.repository.AdminMemberRepository;
import com.tbm.admin.repository.DepartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamManageService {

    private final AdminMemberRepository adminMemberRepository;
    private final DepartRepository departRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<AdminMember> getAdminMembers(Pageable pageable) {
        return adminMemberRepository.findAll(pageable);
    }

    public void addAdminMember(String name, String email, String role, String depart, String password) {
        // 중복 검사 - 이메일로 로그인하니 이메일만 하면될듯
        AdminMember existAdminMember = adminMemberRepository.findByEmail(email);
        if(existAdminMember != null) {
            throw new TbmAdminRuntimeException("이미 등록된 이메일이 있습니다.");
        }

        AdminMember adminMember = new AdminMember();
        adminMember.setName(name);
        adminMember.setEmail(email);
        adminMember.setRole(role);
        adminMember.setDepart(depart);
        adminMember.setStatus("active");
        adminMember.setPassword(passwordEncoder.encode(password));
        adminMember.setCreatedAt(LocalDateTime.now());
        adminMember.setUpdatedAt(LocalDateTime.now());
        adminMemberRepository.save(adminMember);
    }

    public void modifyAdminMember(AdminMember adminMember, String name, String email, String role, String depart, String password) {
        // 중복 검사 - 이메일로 로그인하니 이메일만 하면될듯
        adminMember.setName(name);
        adminMember.setEmail(email);
        adminMember.setRole(role);
        adminMember.setDepart(depart);
        adminMember.setPassword(password);
        adminMemberRepository.save(adminMember);
    }

    public void deleteAdminMember(Long seq) {
        AdminMember adminMember = adminMemberRepository.findBySeq(seq);
        if(adminMember == null) {
            throw new TbmAdminRuntimeException("잘못된 요청입니다.");
        }
        adminMemberRepository.delete(adminMember);
    }

    public AdminMember getMember(Long seq) {
        AdminMember adminMember = adminMemberRepository.findBySeq(seq);
        if(adminMember == null) {
            throw new TbmAdminRuntimeException("잘못된 요청입니다.");
        }
        return adminMember;
    }

    public List<String> getAllDepartments() {
        return departRepository.findAllByOrderByDepartNameAsc()
                .stream()
                .map(Depart::getDepartName)
                .collect(Collectors.toList());
    }


}
