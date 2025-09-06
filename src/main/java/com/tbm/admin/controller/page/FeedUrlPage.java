package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.base.FeedUrlView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.FeedUrlFrontService;
import com.tbm.admin.service.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/feed")
public class FeedUrlPage {

    private final FeedUrlFrontService feedUrlFrontService;
    private final TelegramService telegramService;

    @GetMapping("/url")
    public String getRssUrl(Model model,  @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        return "feed_url_list";
    }

    @GetMapping("/url/form")
    public String getRssUrlForm(@Authed AdminMember adminMember, Model model) {
        model.addAttribute("adminMember", adminMember);
        return "feed_url_form";
    }

    @GetMapping("/url/form/{seq}")
    public String getRssUrlForm(Model model, @Authed AdminMember adminMember, @PathVariable Long seq) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("feedUrl", feedUrlFrontService.getFeedUrl(seq));
        return "feed_url_form";
    }

    @PostMapping("/url")
    @ResponseBody
    public DataTableView getRssUrlList(@RequestBody(required = false)MultiValueMap<String, String> param, @Authed AdminMember adminMember) {
        return feedUrlFrontService.getFeedUrlList(param, adminMember);
    }

    @PostMapping("/url/upsert")
    @ResponseBody
    public RestResult upsertRssUrl(@RequestBody FeedUrlView feedUrlView, @Authed AdminMember adminMember) {
        if (!feedUrlView.getBlogUrl().contains("https://")) {
            telegramService.sendTelegram("블로그 주소는 https:// 로 시작해야 합니다.");
            throw new TbmAdminRuntimeException("블로그 주소는 https:// 로 시작해야 합니다.");
        }
        return feedUrlFrontService.upsertFeedUrl(feedUrlView, adminMember.getSeq());
    }

    @DeleteMapping("/url/delete/{seq}")
    @ResponseBody
    public RestResult deleteRssUrl(@PathVariable Long seq, @Authed AdminMember adminMember) {
        return feedUrlFrontService.deleteFeedUrl(seq, adminMember);
    }
}
