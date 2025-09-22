package com.shinsegaelaw.admin.controller.page;

import com.shinsegaelaw.admin.config.security.Authed;
import com.shinsegaelaw.admin.model.entity.AdminMember;
import com.shinsegaelaw.admin.service.front.MainHelloFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// todo need refactoring !
@Profile("admin")
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainAdminPage extends DefaultAdminPage {

    private final MainHelloFrontService mainHelloFrontService;

    @GetMapping("/")
    public String adminMain(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("message", mainHelloFrontService.mainHello());
        model.addAttribute("isDevOrLocal", isDevOrLocal());
        return "index";
    }
}
