package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.base.ScrapUrlView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.*;
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
public class GpRankingPage {

    private final GpRankingFrontService gpRankingFrontService;
    private final AccountInfoService accountInfoService;
    private final ScrapUrlFrontService scrapUrlFrontService;
    private final ScrapUrlService scrapUrlService;
    private final ScrapQueueFrontService scrapQueueFrontService;
    private final ScrapHistoryFrontService scrapHistoryFrontService;

    // 계정
    @GetMapping("/gp_ranking")
    public String AccountListPage(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "gp_ranking_list";
    }

    @PostMapping("/gp_ranking")
    @ResponseBody
    public DataTableView gpRanking(@Authed AdminMember adminMember, @RequestBody(required = false)MultiValueMap<String, String> param) {
        return gpRankingFrontService.getGpBlogRankings(param, adminMember.getSeq());
    }

    @PostMapping("/gp_keyword_ranking")
    @ResponseBody
    public RestResult gpKeywordRanking(@Authed AdminMember adminMember, @RequestBody(required = false)MultiValueMap<String, String> param) throws Exception {
        return gpRankingFrontService.getGpBlogKeywordRankings(param, adminMember.getSeq());
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

}
