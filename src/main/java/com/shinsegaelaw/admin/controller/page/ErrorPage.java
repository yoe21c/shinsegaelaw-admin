package com.shinsegaelaw.admin.controller.page;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class ErrorPage implements ErrorController {

    @GetMapping({"/error", "/error-page"})
    public String error(@RequestParam(required = false, defaultValue = "404") int code, Model model, HttpServletRequest request) {
        String errMsg = (String) request.getAttribute("errMsg");
        model.addAttribute("code", code);
        model.addAttribute("errMsg", errMsg);
        model.addAttribute("error", true);
        return "login";
    }

}