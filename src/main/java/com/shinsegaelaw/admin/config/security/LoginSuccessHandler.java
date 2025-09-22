package com.shinsegaelaw.admin.config.security;

import com.shinsegaelaw.admin.model.entity.AdminMember;
import com.shinsegaelaw.admin.model.entity.AdminMemberHistory;
import com.shinsegaelaw.admin.repository.AdminMemberHistoryRepository;
import com.shinsegaelaw.admin.service.persist.MemberService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final MemberService memberService;
    private final Environment environment;
    private final AdminMemberHistoryRepository adminMemberHistoryRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {

        AdminMember adminMember = (AdminMember) authentication.getPrincipal();
        if(adminMember.getStatus().equalsIgnoreCase("closed")){
            String errMsg = "잠긴계정입니다. 관리자에게 문의해주세요.";
            httpServletRequest.setAttribute("errMsg", errMsg);
            throw new RuntimeException(errMsg);
        }
        adminMember.setUpdatedAt(LocalDateTime.now());
        memberService.save(adminMember);
        httpServletRequest.getSession().setAttribute("member", adminMember);

        // 로그인 이력 저장
        adminMemberHistoryRepository.save(new AdminMemberHistory(adminMember.getSeq(), "login", "로그인", "로그인 성공"));

        super.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);
    }

    private boolean isFront() {
        for (String activeProfile : environment.getActiveProfiles()) {
            if(activeProfile.equalsIgnoreCase("front")) {
                return true;
            }
        }
        return false;
    }
}