package com.tbm.admin.service.front;

import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.ScrapHistory;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.base.ScrapHistoryView;
import com.tbm.admin.service.persist.ScrapHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapHistoryFrontService {

    private final ScrapHistoryService scrapHistoryService;

    public DataTableView getScrapHistory(MultiValueMap<String, String> param, AdminMember adminMember) {

        int draw = Integer.parseInt(param.get("draw").get(0));
        int start = Integer.parseInt(param.get("start").get(0));
        int length = Integer.parseInt(param.get("length").get(0));
        String search = param.get("search[value]").get(0);

        String keyword = StringUtils.isNotBlank(param.get("keyword").get(0)) ? param.get("keyword").get(0) : "";

        String displayStatus = param.get("status").get(0);

        int pageNumber = (start / length);
        final PageRequest pageable = PageRequest.of(pageNumber, length, Sort.Direction.DESC, "seq");

        Long memberSeq = adminMember.getSeq();
        if(adminMember.isSupperAdmin()) {
            memberSeq = null;
        }

        final Page<ScrapHistoryView> scrapHistoryViewPage;
        final Page<ScrapHistory> scrapHistoryPage;
        if(displayStatus.equalsIgnoreCase("success")) {
            scrapHistoryPage = scrapHistoryService.getAllWithoutFail(keyword, memberSeq, pageable);
        } else if (displayStatus.equalsIgnoreCase("fail")) {
            scrapHistoryPage = scrapHistoryService.getAllWithoutSuccess(keyword, memberSeq, pageable);
        } else {
            scrapHistoryPage = scrapHistoryService.getAllWith(keyword, memberSeq, pageable);
        }

        final List<Long> scrapHistorySequences = scrapHistoryPage.getContent().stream().map(ScrapHistory::getSeq).collect(Collectors.toList());
        final List<ScrapHistory> scrapHistories = scrapHistoryPage.getContent();
        List<ScrapHistoryView> results = new ArrayList<>();

        for (ScrapHistory scrapHistory : scrapHistories) {
            final ScrapHistoryView result = new ScrapHistoryView();
            result.setSeq(scrapHistory.getSeq());
            if(adminMember.isSupperAdmin()) {
                result.setAccountId(scrapHistory.getAccountId());
            }else {
                result.setAccountId("******");
            }
            result.setBlogUrl(scrapHistory.getBlogUrl());
            result.setResult(scrapHistory.getResult());
            result.setDescription(scrapHistory.getDescription());
            result.setCreatedAt(scrapHistory.getCreatedAt());
            results.add(result);
        }
        scrapHistoryViewPage = new PageImpl<>(results, scrapHistoryPage.getPageable(), scrapHistoryPage.getTotalElements());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scrapHistory", scrapHistoryViewPage);

        return new DataTableView(draw, scrapHistoryViewPage.getTotalElements(), scrapHistoryViewPage.getTotalElements(), data);
    }
}
