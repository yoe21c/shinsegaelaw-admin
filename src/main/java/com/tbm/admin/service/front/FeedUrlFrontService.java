package com.tbm.admin.service.front;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.FeedUrl;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.base.FeedUrlView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.persist.FeedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.tbm.admin.utils.Utils.toJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedUrlFrontService {

    private final FeedUrlService feedUrlService;

    public DataTableView getFeedUrlList(MultiValueMap<String, String> param, AdminMember adminMember) {
        int draw = Integer.parseInt(param.get("draw").getFirst());
        int start = Integer.parseInt(param.get("start").getFirst());
        int length = Integer.parseInt(param.get("length").getFirst());

        String keyword = StringUtils.isNotBlank(param.get("keyword").getFirst()) ? param.get("keyword").getFirst() : null;

        int pageNumber = (start / length);
        final PageRequest pageable = PageRequest.of(pageNumber, length, Sort.Direction.DESC, "seq");

        Long memberSeq = adminMember.getSeq();
        if (adminMember.isSupperAdmin()) {
            memberSeq = null;
        }

        Page<FeedUrl> feedUrlPage = feedUrlService.getAllWith(keyword, memberSeq, pageable);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("feedUrl", feedUrlPage);

        return new DataTableView(draw, feedUrlPage.getTotalElements(), feedUrlPage.getTotalElements(), data);


    }

    public FeedUrl getFeedUrl(Long seq) {
        return feedUrlService.getFeedUrl(seq);
    }

    public RestResult upsertFeedUrl(FeedUrlView feedUrlView, Long memberSeq) {
        log.info("--> upsert Feed Url : {}", toJson(feedUrlView));

        FeedUrl feedUrl = feedUrlService.getFeedUrlByBlogUrl(feedUrlView.getBlogUrl());
        if (feedUrl == null) {
            if (feedUrlView.getSeq() == null) {
                feedUrl = new FeedUrl();
            } else {
                feedUrl = feedUrlService.getFeedUrl(feedUrlView.getSeq());
            }
        }
        feedUrl.setMemberSeq(memberSeq);
        String blogUrl = feedUrlView.getBlogUrl().trim();
        feedUrl.setBlogUrl(blogUrl);
        feedUrl.setTargetScrapCount(feedUrlView.getTargetScrapCount());
        feedUrl.setMonitoringCount(feedUrlView.getMonitoringCount());
        feedUrl.setReservedAt(feedUrlView.getReservedAt());
        feedUrlService.save(feedUrl);

        log.info("Complete ! upsert Feed Url");

        return RestResult.success();
    }

    public RestResult deleteFeedUrl(Long seq, AdminMember adminMember) {
        FeedUrl feedUrl = feedUrlService.getFeedUrl(seq);
        if (feedUrl.getMonitoringCount() > 0) {
            throw new TbmAdminRuntimeException("모니터링 중인 URL은 삭제할 수 없습니다.");
        }
        feedUrlService.deleteFeedUrl(seq);
        log.info("Complete ! delete Feed ! seq: {}, feedUrl: {}", seq, toJson(feedUrl));
        return RestResult.success();
    }
}
