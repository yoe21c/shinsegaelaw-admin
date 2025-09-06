package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.service.front.BlogMatrixFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BlogMatrixPage {

    private final BlogMatrixFrontService blogMatrixFrontService;

    // 계정
    @GetMapping("/blog_matrix")
    public String AccountListPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("blogMatrix", blogMatrixFrontService.searchUserInfo(adminMember.getSeq()));
        return "blog_matrix";
    }

    @PostMapping("/blog_matrix")
    @ResponseBody
    public DataTableView blogMatrix(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        return blogMatrixFrontService.searchBlogMatrix(param, adminMember.getSeq());
    }

}
