package com.shinsegaelaw.admin.controller.page;

import com.shinsegaelaw.admin.config.security.Authed;
import com.shinsegaelaw.admin.model.entity.AdminMember;
import com.shinsegaelaw.admin.model.view.base.DataTableView;
import com.shinsegaelaw.admin.service.front.MemberHistoryFrontService;
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
public class MemberHistoryPage {

    private final MemberHistoryFrontService memberHistoryFrontService;

    @GetMapping("/history")
    public String historyPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "history";
    }

    @PostMapping("/history")
    @ResponseBody
    public DataTableView history(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        log.info("/history ? adminMemberSeq: {}", adminMember.getSeq());
        return memberHistoryFrontService.getMemberHistory(param, adminMember.getSeq());
    }

}