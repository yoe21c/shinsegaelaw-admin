package com.shinsegaelaw.admin.controller.page;

import com.shinsegaelaw.admin.config.security.Authed;
import com.shinsegaelaw.admin.model.entity.AdminMember;
import com.shinsegaelaw.admin.model.param.StringMultiValueMapAdapter;
import com.shinsegaelaw.admin.model.view.base.DataTableView;
import com.shinsegaelaw.admin.model.view.rest.RestResult;
import com.shinsegaelaw.admin.service.front.RepairAgentFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RepairAgentPage {

    private final RepairAgentFrontService repairAgentFrontService;

    // 리페어 에이전트 페이지 조회
    @GetMapping("/repair_agent_list")
    public String repairAgents(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "repair_agent_list";
    }

    // 리페어 에이전트 리스트 조회
    @PostMapping("/repair_agent_list")
    @ResponseBody
    public DataTableView repairAgents(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        return repairAgentFrontService.getRepairAgents(param, adminMember.getSeq());
    }

    @GetMapping("/repair_agent/{seq}")
    public String repairAgent(Model model, @Authed AdminMember adminMember, @PathVariable Long seq) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("repairAgent", repairAgentFrontService.getRepairAgent(seq));
        return "repair_agent";
    }

    @GetMapping("/repair_agent_create")
    public String repairAgentCreate(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "repair_agent";
    }

    @PostMapping("/repair_agent_update")
    @ResponseBody
    public RestResult repairAgentUpdate(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        log.debug("/repair_agent_update {}", param);
        repairAgentFrontService.updateRepairAgent(new StringMultiValueMapAdapter(param));
        return RestResult.success();
    }

    @PostMapping("/repair_agent_delete")
    @ResponseBody
    public RestResult repairAgentDelete(@Authed AdminMember adminMember, @RequestParam Long seq) {
        log.debug("/repair_agent_delete seq: {}", seq);
        repairAgentFrontService.deleteRepairAgent(seq);
        return RestResult.success();
    }

}
