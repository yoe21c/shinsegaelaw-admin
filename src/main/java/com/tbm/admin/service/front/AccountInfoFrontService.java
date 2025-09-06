package com.tbm.admin.service.front;

import com.tbm.admin.config.AesConfig;
import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.view.base.AccountInfoView;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.persist.AccountInfoService;
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
public class AccountInfoFrontService {

    private final AccountInfoService accountInfoService;

    private final AesConfig aesConfig;

    public RestResult mappingAccountInfo(String ipAddress) {
        final AccountInfo accountInfo = accountInfoService.mappingAccountInfo(ipAddress);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountInfo", accountInfo);
        return new RestResult(data);
    }

    public DataTableView getAccountInfo(MultiValueMap<String, String> param, Long memberSeq) {

        int draw = Integer.parseInt(param.get("draw").get(0));
        int start = Integer.parseInt(param.get("start").get(0));
        int length = Integer.parseInt(param.get("length").get(0));
        String search = param.get("search[value]").get(0);

        String keyword = StringUtils.isNotBlank(param.get("keyword").get(0)) ? param.get("keyword").get(0) : "";

        String displayStatus = param.get("status").get(0);

        int pageNumber = (start / length);
        final PageRequest pageable = PageRequest.of(pageNumber, length, Sort.Direction.DESC, "seq");

        final Page<AccountInfoView> accountInfoViewPage;
        final Page<AccountInfo> accountInfoPage;
        if(displayStatus.equalsIgnoreCase("active")) {
            accountInfoPage = accountInfoService.getAllWithoutUnassignedIp(keyword, pageable);
        } else if (displayStatus.equalsIgnoreCase("inactive")) {
            accountInfoPage = accountInfoService.getAllWithoutAssignedIp(keyword, pageable);
        } else {
            accountInfoPage = accountInfoService.getAllWith(keyword, pageable);
        }

        final List<Long> accountInfoSequences = accountInfoPage.getContent().stream().map(AccountInfo::getSeq).collect(Collectors.toList());
        final List<AccountInfo> accountInfos = accountInfoPage.getContent();
        List<AccountInfoView> results = new ArrayList<>();

        for (AccountInfo accountInfo : accountInfos) {
            final AccountInfoView result = new AccountInfoView();
            result.setSeq(accountInfo.getSeq());
            result.setAssignment(accountInfo.getIpAddress() == null ? "미할당" : "활동중");
            result.setId(accountInfo.getId());
            result.setPassword(accountInfo.getPassword());
            result.setIpAddress(accountInfo.getIpAddress());
            result.setDailyCount(accountInfo.getDailyCount());
            result.setDailyCountLimit(accountInfo.getDailyCountLimit());
            result.setCreatedAt(accountInfo.getCreatedAt());
            results.add(result);
        }
        accountInfoViewPage = new PageImpl<>(results, accountInfoPage.getPageable(), accountInfoPage.getTotalElements());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountInfo", accountInfoViewPage);

        return new DataTableView(draw, accountInfoViewPage.getTotalElements(), accountInfoViewPage.getTotalElements(), data);
    }

    public RestResult upsertAccount(AccountInfoView view) {
        AccountInfo accountInfo = accountInfoService.getAccountInfo(view.getSeq());
        accountInfo.setId(view.getId());
        accountInfo.setPassword(aesConfig.encryption(view.getPassword()));
        accountInfoService.save(accountInfo);

        return RestResult.success();
    }
}
