package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.Depart;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.DepartFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DepartManagePage {
    
    private final DepartFrontService departFrontService;
    
    @GetMapping("/depart_manage")
    public String departManagePage(Model model, @Authed AdminMember adminMember,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        model.addAttribute("adminMember", adminMember);

        Page<Depart> departs = departFrontService.getDeparts(page, size);
        model.addAttribute("departs", departs);
        return "depart_manage";
    }
    
    @GetMapping("/depart_manage/departs")
    @ResponseBody
    public Page<Depart> getDeparts(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return departFrontService.getDeparts(page, size);
    }

    @PostMapping("/depart_manage/add_depart")
    @ResponseBody
    public RestResult addDepart(@RequestBody Map<String, Object> body) {
        return departFrontService.addDepart(body);
    }

    @PostMapping("/depart_manage/modify_depart")
    @ResponseBody
    public RestResult modifyDepart(@RequestBody Map<String, Object> body) {
        return departFrontService.modifyDepart(body);
    }

    @PostMapping("/depart_manage/delete_depart")
    @ResponseBody
    public RestResult deleteDepart(@RequestBody Map<String, Object> body) {
        return departFrontService.deleteDepart(body);
    }

    @GetMapping("/depart_manage/depart")
    @ResponseBody
    public RestResult getDepart(@RequestParam("seq") Long seq) {
        return departFrontService.getDepart(seq);
    }
} 