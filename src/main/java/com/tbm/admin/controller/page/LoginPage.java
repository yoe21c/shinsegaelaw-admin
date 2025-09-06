package com.tbm.admin.controller.page;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class LoginPage extends DefaultAdminPage {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) boolean error, Model model) {

        if(SecurityContextHolder.getContext() != null &&
           SecurityContextHolder.getContext().getAuthentication() != null &&
           SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
           ! SecurityContextHolder.getContext().getAuthentication().getPrincipal().equals("anonymousUser")
        ) {
            return "redirect:/";
        }
        if(error) {
            model.addAttribute("error", true);
        }
        return "login";
    }
}
