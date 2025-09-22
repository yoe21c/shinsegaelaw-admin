package com.shinsegaelaw.admin.config.security;

import com.shinsegaelaw.admin.model.entity.AdminMember;
import com.shinsegaelaw.admin.model.entity.AdminMemberHistory;
import com.shinsegaelaw.admin.repository.AdminMemberHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MyLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AdminMemberHistoryRepository adminMemberHistoryRepository;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        AdminMember adminMember = (AdminMember) authentication.getPrincipal();

        // 로그아웃 이력 저장
        adminMemberHistoryRepository.save(new AdminMemberHistory(adminMember.getSeq(), "logout", "로그아웃", "로그아웃 성공"));

        response.sendRedirect("/login");
    }
}