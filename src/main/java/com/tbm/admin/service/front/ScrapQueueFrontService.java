package com.tbm.admin.service.front;

import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.service.persist.ScrapQueueService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapQueueFrontService {

    private final ScrapQueueService scrapQueueService;

    public DataTableView getScrapQueue(MultiValueMap<String, String> param, AdminMember adminMember) {

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
        String role = adminMember.getRole();

        final Page<ScrapQueue> scrapQueues = scrapQueueService.findAllWith(status, keyword, memberSeq, role, pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scrapQueue", scrapQueues);

        return new DataTableView(draw, scrapQueues.getTotalElements(), scrapQueues.getTotalElements(), data);
    }

}
