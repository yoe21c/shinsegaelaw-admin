package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.Depart;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.TeamManageFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TeamManagePage {

    private final TeamManageFrontService teamManageFrontService;

    @GetMapping("/team_management")
    public String teamManagement(Model model,
                                     @Authed AdminMember adminMember,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        model.addAttribute("adminMember", adminMember);
        Page<AdminMember> teamMembers = teamManageFrontService.getTeamMembers(page, size);

        List<Depart> departs = teamManageFrontService.getDepartments();
        model.addAttribute("teams", teamMembers);
        model.addAttribute("departs", departs);
        return "team_manage";
    }

    @GetMapping("/members")
    @ResponseBody
    public Page<AdminMember> getMembers(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return teamManageFrontService.getTeamMembers(page, size);
    }

    @PostMapping("/add_member")
    @ResponseBody
    public RestResult addMember(@RequestBody Map<String, Object> body) {
        return teamManageFrontService.addMember(body);
    }

    @PostMapping("/modify_member")
    @ResponseBody
    public RestResult modifyMember(@RequestBody Map<String, Object> body) {
        return teamManageFrontService.modifyMember(body);
    }

    @PostMapping("/delete_member")
    @ResponseBody
    public RestResult deleteMember(@RequestBody Map<String, Object> body) {
        return teamManageFrontService.deleteAdminMember(body);
    }

    @GetMapping("/member")
    @ResponseBody
    public RestResult getMember(@RequestParam("seq") Long seq) {
        return teamManageFrontService.getMember(seq);
    }
}
