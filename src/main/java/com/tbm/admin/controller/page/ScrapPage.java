package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.view.base.AccountInfoView;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.base.ScrapUrlView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.AccountInfoFrontService;
import com.tbm.admin.service.front.ScrapHistoryFrontService;
import com.tbm.admin.service.front.ScrapQueueFrontService;
import com.tbm.admin.service.front.ScrapUrlFrontService;
import com.tbm.admin.service.persist.AccountInfoService;
import com.tbm.admin.service.persist.ScrapUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/scrap")
public class ScrapPage {

    private final AccountInfoFrontService accountInfoFrontService;
    private final AccountInfoService accountInfoService;
    private final ScrapUrlFrontService scrapUrlFrontService;
    private final ScrapUrlService scrapUrlService;
    private final ScrapQueueFrontService scrapQueueFrontService;
    private final ScrapHistoryFrontService scrapHistoryFrontService;

    // 계정
    @GetMapping("/account")
    public String AccountListPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "account_info_list";
    }

    @PostMapping("/account")
    @ResponseBody
    public DataTableView account(@Authed AdminMember adminMember, @RequestBody(required = false)MultiValueMap<String, String> param) {
        return accountInfoFrontService.getAccountInfo(param, adminMember.getSeq());
    }

    @GetMapping("/account/form")
    public String AccountFormPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "account_info_form";
    }

    @GetMapping("/account/form/{seq}")
    public String AccountFormPage(Model model, @Authed AdminMember adminMember, @PathVariable Long seq) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("account", accountInfoService.getAccountInfo(seq));
        return "account_info_form";
    }

    @PostMapping("/account/upsert")
    @ResponseBody
    public RestResult upsertUrl(@RequestBody AccountInfoView view) {
        return accountInfoFrontService.upsertAccount(view);
    }

    // 블로그 url
    @GetMapping("/url")
    public String UrlListPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "scrap_url_list";
    }

    @PostMapping("/url")
    @ResponseBody
    public DataTableView url(@Authed AdminMember adminMember, @RequestBody(required = false)MultiValueMap<String, String> param) {
        return scrapUrlFrontService.getScrapUrl(param, adminMember);
    }

    @GetMapping("/url/form")
    public String UrlFormPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("totalCount", accountInfoService.countAssignedAccountInfo());
        model.addAttribute("availableCount", accountInfoService.countAvailableAccountInfo());
        return "scrap_url_form";
    }

    @GetMapping("/url/form/{seq}")
    public String UrlFormPage(Model model, @Authed AdminMember adminMember, @PathVariable Long seq) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("url", scrapUrlService.getScrapUrl(seq));
        return "scrap_url_form";
    }

    @PostMapping("/url/upsert")
    @ResponseBody
    public RestResult upsertUrl(@RequestBody ScrapUrlView view) {
        return scrapUrlFrontService.upsertUrl(view);
    }

    @PostMapping("/url-list/upsert")
    @ResponseBody
    public RestResult upsertUrlCsv(@RequestBody List<ScrapUrlView> views, @Authed AdminMember adminMember) {
        return scrapUrlFrontService.upsertUrlList(views, adminMember);
    }

    // 스크랩 대기열 조회
    @GetMapping("/queue")
    public String scrapQueuePage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "scrap_queue_list";
    }

    @PostMapping("/queue")
    @ResponseBody
    public DataTableView queue(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        return scrapQueueFrontService.getScrapQueue(param, adminMember);
    }

    // 스크랩 내역 조회
    @GetMapping("/history")
    public String scrapHistoryPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "scrap_history";
    }

    @PostMapping("/history")
    @ResponseBody
    public DataTableView history(@Authed AdminMember adminMember, @RequestBody(required = false) MultiValueMap<String, String> param) {
        return scrapHistoryFrontService.getScrapHistory(param, adminMember);
    }

    @GetMapping("/history/{keyword}")
    public String scrapHistoryPageSearch(Model model, @Authed AdminMember adminMember, @PathVariable String keyword) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("keyword", keyword);
        return "scrap_history";
    }

}
