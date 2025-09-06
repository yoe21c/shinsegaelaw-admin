package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.RepairAgentQueue;
import com.tbm.admin.model.param.StringMultiValueMapAdapter;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.RepairAgentFrontService;
import com.tbm.admin.service.front.RepairAgentQueueFrontService;
import com.tbm.admin.service.thirdparty.BlogTitleExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RepairAgentQueuePage {

    private final RepairAgentFrontService repairAgentFrontService;
    private final RepairAgentQueueFrontService repairAgentQueueFrontService;
    private final BlogTitleExtractor blogTitleExtractor;

    // 리페어 에이전트 페이지 조회
    @GetMapping("/repair_agent_queue_list")
    public String repairAgentQueues(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "repair_agent_queue_list";
    }

    // 리페어 에이전트 리스트 조회
    @PostMapping("/repair_agent_queue_list")
    @ResponseBody
    public DataTableView repairAgentQueues(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        return repairAgentQueueFrontService.getRepairAgentQueues(param, adminMember.getSeq());
    }

    @GetMapping("/repair_agent_queue/{seq}")
    public String repairAgentQueue(Model model, @Authed AdminMember adminMember, @PathVariable Long seq) {

        final RepairAgentQueue repairAgentQueue = repairAgentQueueFrontService.getRepairAgentQueue(seq);
        final String title = blogTitleExtractor.extractTitle(repairAgentQueue.getBlogUrl());
        repairAgentQueue.setCurrentTitle(title);
//        if(repairAgentQueue.isDeleted()) {
//            return "redirect:/repair_agent_queue_list";
//        }
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("repairAgentQueue", repairAgentQueue);
        return "repair_agent_queue";
    }

    @GetMapping("/repair_agent_queue_create")
    public String repairAgentQueueCreate(Model model,
                                         @RequestParam(required = false) Long seq,
                                         @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);

        // seq 가 있으면 복제하는것이다.
        if(seq != null) {
            final RepairAgentQueue repairAgentQueue = repairAgentQueueFrontService.getRepairAgentQueue(seq);
            repairAgentQueue.setSeq(null);  // 복제하기 때문에 없는것이다.
            repairAgentQueue.setReservedAt(repairAgentQueue.getReservedAt().plusHours(1));
            repairAgentQueue.setStatus("reserved");
            model.addAttribute("repairAgentQueue", repairAgentQueue);
        }
        return "repair_agent_queue";
    }

    @PostMapping("/repair_agent_queue_update")
    @ResponseBody
    public RestResult repairAgentQueueUpdate(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        log.info("/repair_agent_queue_update {}", param);
        repairAgentQueueFrontService.updateRepairAgentQueue(new StringMultiValueMapAdapter(param));
        return RestResult.success();
    }

    @PostMapping("/repair_agent_queue_delete")
    @ResponseBody
    public RestResult repairAgentQueueDelete(@RequestParam Long seq, @Authed AdminMember adminMember) {
        log.info("/repair_agent_queue_delete {}", seq);
        repairAgentQueueFrontService.deleteRepairAgentQueue(seq);
        return RestResult.success();
    }

    @PostMapping("/import_repairs")
    @ResponseBody
    public RestResult importRepairs(@RequestParam("file") MultipartFile file, @Authed AdminMember adminMember) {
        log.info("/import_repairs payload: {}", file);
        return repairAgentQueueFrontService.importRepairs(file);
    }

    @PostMapping("/finalize_import_repairs")
    @ResponseBody
    public RestResult finalizeImportRepairs(@RequestParam("file") MultipartFile file, @Authed AdminMember adminMember) {
        log.info("/finalize_import_repairs payload: {}", file);
        final RestResult restResult = repairAgentQueueFrontService.importRepairs(file);

        List<RepairAgentQueue> newRepairAgentQueues = (List<RepairAgentQueue>) restResult.getData().get("newRepairAgentQueues");

        return repairAgentQueueFrontService.finalizeImportReports(newRepairAgentQueues);
    }
}
