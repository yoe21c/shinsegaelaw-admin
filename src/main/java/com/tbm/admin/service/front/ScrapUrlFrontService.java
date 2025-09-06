package com.tbm.admin.service.front;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.entity.ScrapUrl;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.base.ScrapUrlView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.repository.AccountInfoRepository;
import com.tbm.admin.repository.ScrapQueueRepository;
import com.tbm.admin.service.persist.AccountInfoService;
import com.tbm.admin.service.persist.ScrapUrlService;
import com.tbm.admin.service.scrap.ScrapAgentRequester;
import com.tbm.admin.service.scrap.ScrapUrlRequester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tbm.admin.utils.Utils.toJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapUrlFrontService {

    private final ScrapUrlService scrapUrlService;
    private final ScrapQueueRepository scrapQueueRepository;
    private final ScrapUrlRequester scrapUrlRequester;

    public DataTableView getScrapUrl(MultiValueMap<String, String> param, AdminMember adminMember) {

        int draw = Integer.parseInt(param.get("draw").get(0));
        int start = Integer.parseInt(param.get("start").get(0));
        int length = Integer.parseInt(param.get("length").get(0));
        String search = param.get("search[value]").get(0);

        String keyword = StringUtils.isNotBlank(param.get("keyword").get(0)) ? param.get("keyword").get(0) : null;

        String status = param.get("status").get(0);
        if(status.equals("all")) {
            status = null;
        }

        int pageNumber = (start / length);
        final PageRequest pageable = PageRequest.of(pageNumber, length, Sort.Direction.DESC, "seq");

        Long memberSeq = adminMember.getSeq();
        if(adminMember.isSupperAdmin()) {
            memberSeq = null;
        }

        final Page<ScrapUrl> scrapUrlPage = scrapUrlService.getAllWith(status, keyword, memberSeq, pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scrapUrl", scrapUrlPage);

        return new DataTableView(draw, scrapUrlPage.getTotalElements(), scrapUrlPage.getTotalElements(), data);
    }

    /**
     * 단건 등록하기 (미사용)
     * @param view
     * @return
     */
    public RestResult upsertUrl(ScrapUrlView view) {

        log.info("--> upsertUrl view : {}", toJson(view));

        ScrapUrl scrapUrl = scrapUrlService.getScrapUrl(view.getSeq());
        scrapUrl.setStartAt(view.getStartAt());
        scrapUrl.setEndAt(view.getStartAt().plusDays(1));
        scrapUrl.setBlogUrl(view.getBlogUrl());
        scrapUrl.setActivate(view.getActivate());
        scrapUrl.setTargetCount(view.getTargetCount());
        scrapUrlService.save(scrapUrl);

        final List<ScrapQueue> scrapQueues = scrapQueueRepository.findByUrlSeq(scrapUrl.getSeq());

        // todo 숫자를 받아서 처리하도록 수정해야한다.
        for (ScrapQueue scrapQueue : scrapQueues) {
            scrapQueue.setStartAt(scrapUrl.getStartAt());
            scrapQueue.setEndAt(scrapUrl.getEndAt());
            scrapQueueRepository.save(scrapQueue);
        }

        log.info("Complete ! upsertUrl view");

        return RestResult.success();
    }

    /**
     * 다중으로 등록하기 (사용중)
     *
     * @param views
     * @param adminMember
     * @return
     */
    public RestResult upsertUrlList(List<ScrapUrlView> views, AdminMember adminMember) {

        log.info("--> upsertUrlList views : {}", toJson(views));
        scrapUrlRequester.requestScrapUrl(views, adminMember);
        return RestResult.success();
    }

}
