package com.shinsegaelaw.admin.controller.page;

import com.shinsegaelaw.admin.config.security.Authed;
import com.shinsegaelaw.admin.model.entity.AdminMember;
import com.shinsegaelaw.admin.model.param.StringMultiValueMapAdapter;
import com.shinsegaelaw.admin.model.view.base.DataTableView;
import com.shinsegaelaw.admin.model.view.rest.RestResult;
import com.shinsegaelaw.admin.service.front.CounselFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CounselPage {

    private final CounselFrontService counselFrontService;

    // 상담내역 페이지 조회
    @GetMapping("/counsel_list")
    public String counsels(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "counsel_list";
    }

    // 상담내역 리스트 조회
    @PostMapping("/counsel_list")
    @ResponseBody
    public DataTableView counsels(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        return counselFrontService.getCounsels(param, adminMember.getSeq());
    }

    @GetMapping("/counsel/{seq}")
    public String counsel(Model model, @Authed AdminMember adminMember, @PathVariable Long seq) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("counsel", counselFrontService.getCounsel(seq));
        return "counsel";
    }

    @GetMapping("/counsel_create")
    public String counselCreate(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "counsel";
    }

    @PostMapping("/counsel_update")
    @ResponseBody
    public RestResult counselUpdate(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        log.debug("/counsel_update {}", param);
        counselFrontService.updateCounsel(new StringMultiValueMapAdapter(param));
        return RestResult.success();
    }

    @PostMapping("/counsel_delete")
    @ResponseBody
    public RestResult counselDelete(@Authed AdminMember adminMember, @RequestParam Long seq) {
        log.debug("/counsel_delete seq: {}", seq);
        counselFrontService.deleteCounsel(seq);
        return RestResult.success();
    }

}