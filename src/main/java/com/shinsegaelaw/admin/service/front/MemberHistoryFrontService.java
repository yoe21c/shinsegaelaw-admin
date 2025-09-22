package com.shinsegaelaw.admin.service.front;

import com.shinsegaelaw.admin.model.entity.AdminMember;
import com.shinsegaelaw.admin.model.entity.AdminMemberHistory;
import com.shinsegaelaw.admin.model.view.base.DataTableView;
import com.shinsegaelaw.admin.model.view.base.MemberHistoryView;
import com.shinsegaelaw.admin.service.persist.MemberHistoryService;
import com.shinsegaelaw.admin.service.persist.MemberService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberHistoryFrontService {

    private final MemberHistoryService memberHistoryService;
    private final MemberService memberService;

    public DataTableView getMemberHistory(MultiValueMap<String, String> param, Long memberSeq) {

        int draw = Integer.parseInt(param.get("draw").get(0));
        int start = Integer.parseInt(param.get("start").get(0));
        int length = Integer.parseInt(param.get("length").get(0));
        String search = param.get("search[value]").get(0);

        String keyword = StringUtils.isNotBlank(param.get("keyword").get(0)) ? param.get("keyword").get(0) : null;

        String displayStatus = "";
        boolean withoutLoginLogout = false;
        if(! param.get("status").get(0).equals("all")) {
            displayStatus = param.get("status").get(0);
            if(displayStatus.equalsIgnoreCase("withoutLoginLogout")) {
                withoutLoginLogout = true;
            }
        }
        int pageNumber = (start / length);
        final PageRequest pageable = PageRequest.of(pageNumber, length, Sort.Direction.DESC, "seq");

        // 한방쿼리에서 syntax 에러가 fix 가 안되서 우선 2번 쿼리하는 것으로 임시조치.
        final Page<MemberHistoryView> historyPage;
        final Page<AdminMemberHistory> memberHistoryPage;
        if(withoutLoginLogout) {
            memberHistoryPage = memberHistoryService.getAllWithoutLoginLogOut(keyword, pageable);
        }else {
            memberHistoryPage = memberHistoryService.getAllWith(keyword, pageable);
        }

        final List<Long> memberSequences = memberHistoryPage.getContent().stream().map(AdminMemberHistory::getMemberSeq).collect(Collectors.toList());

        final List<AdminMember> allMembers = memberService.getAllMembers();
        final Map<Long, AdminMember> allMembersMap = allMembers.stream().collect(Collectors.toMap(AdminMember::getSeq, Function.identity()));

        final List<AdminMemberHistory> memberHistories = memberHistoryPage.getContent();
        List<MemberHistoryView> results = new ArrayList<>();
        for (AdminMemberHistory adminMemberHistory : memberHistories) {

            final MemberHistoryView result = new MemberHistoryView();
            result.setMemberSeq(adminMemberHistory.getMemberSeq());
            result.setName(allMembersMap.get(adminMemberHistory.getMemberSeq()).getName());
            result.setEmail(memberService.getMemberBySeq(adminMemberHistory.getMemberSeq()).getEmail());
            result.setAction(adminMemberHistory.getAction());
            result.setActionName(adminMemberHistory.getActionName());
            result.setDescription(adminMemberHistory.getDescription());
            result.setCreatedAt(adminMemberHistory.getCreatedAt());
            results.add(result);
        }
        historyPage = new PageImpl<>(results, memberHistoryPage.getPageable(), memberHistoryPage.getTotalElements());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("history", historyPage);

        return new DataTableView(draw, historyPage.getTotalElements(), historyPage.getTotalElements(), data);
    }

}