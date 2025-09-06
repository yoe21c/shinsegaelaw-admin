package com.tbm.admin.service.front;

import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.Depart;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.persist.DepartService;
import com.tbm.admin.service.persist.TeamManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamManageFrontService {

    private final TeamManageService teamManageService;
    private final PasswordEncoder passwordEncoder;
    private final DepartService departService;

    public Page<AdminMember> getTeamMembers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "seq"));
        return teamManageService.getAdminMembers(pageable);
    }

    public RestResult addMember(Map<String, Object> body) {
        String name = body.get("name").toString();
        String email = body.get("email").toString();
        String role = body.get("role").toString();
        String depart = body.get("depart").toString();
        String password = "1234";
        if(body.get("password") != null) {
            password = body.get("password").toString();
        }
        teamManageService.addAdminMember(name, email, role, depart, password);
        return RestResult.success();
    }

    public RestResult modifyMember(Map<String, Object> body) {
        Long seq = Long.valueOf(body.get("seq").toString());
        AdminMember adminMember = teamManageService.getMember(seq);
        String name = body.get("name").toString();
        String email = body.get("email").toString();
        String role = body.get("role").toString();
        String depart = body.get("depart").toString();
        String password = "";
        if(body.get("password") != null) {
            password = body.get("password").toString();
            // 비밀번호는 기존과 비교
            if(!adminMember.getPassword().equals(password)) {
                // 다르면 암호화
                password = passwordEncoder.encode(password);
            }
        } else {
            password = adminMember.getPassword();
        }
        teamManageService.modifyAdminMember(adminMember, name, email, role, depart, password);
        return RestResult.success();
    }

    public RestResult deleteAdminMember(Map<String, Object> body) {
        Long seq = Long.parseLong(body.get("seq").toString());
        teamManageService.deleteAdminMember(seq);
        return RestResult.success();
    }

    public RestResult getMember(Long seq) {
        AdminMember adminMember = teamManageService.getMember(seq);
        Map<String, Object> map = new HashMap<>();
        map.put("member", adminMember);
        return new RestResult(map);
    }

    public List<Depart> getDepartments() {
        return departService.getAllDeparts();
    }

}
