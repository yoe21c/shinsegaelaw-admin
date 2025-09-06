package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.param.MainMessage;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.MainHelloFrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ImageCombinePage {

    private final MainHelloFrontService mainHelloFrontService;

    // 계정
    @GetMapping("/image_combine")
    public String mainHelloPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("message", mainHelloFrontService.mainHello());
        return "image_combine";
    }

    @PostMapping("/image_combine/update")
    @ResponseBody
    public RestResult updateMainHello(@RequestBody MainMessage message) {
        return mainHelloFrontService.upsertMessage(message);
    }

}
