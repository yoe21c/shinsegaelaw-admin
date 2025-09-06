package com.tbm.admin.service.batch;

import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.entity.ScrapUrl;
import com.tbm.admin.model.view.base.ScrapQueueDaily;
import com.tbm.admin.repository.ScrapUrlRepository;
import com.tbm.admin.service.persist.AccountInfoService;
import com.tbm.admin.service.persist.ConfigService;
import com.tbm.admin.service.persist.ScrapQueueService;
import com.tbm.admin.service.persist.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tbm.admin.utils.Utils.toJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final ScrapUrlRepository scrapUrlRepository;
    private final ScrapQueueService scrapQueueService;
    private final AccountInfoService accountInfoService;
    private final SearchKeywordService searchKeywordService;
    private final ConfigService configService;

    public void updateScrapUrlStatus() {

        // url 상태 준비 -> 진행
        List<ScrapUrl> readyUrlList = scrapUrlRepository.findByStatusAndStartAtBeforeAndEndAtAfter("ready", LocalDateTime.now(), LocalDateTime.now());
        for (ScrapUrl scrapUrl: readyUrlList) {
            scrapUrl.setStatus("processing");
            scrapUrlRepository.save(scrapUrl);
        }

        // url 상태 진행 -> 종료
        List<ScrapUrl> processingUrlList = scrapUrlRepository.findByStatusAndEndAtBefore("processing", LocalDateTime.now());
        for (ScrapUrl scrapUrl: processingUrlList) {
            scrapUrl.setStatus("closed");
            scrapUrlRepository.save(scrapUrl);
        }

    }

    public void resetDailyCount() {

        final List<ScrapQueueDaily> scrapQueueDailies = scrapQueueService.getReadyTodayAll();

        Map<String, ScrapQueueDaily> accountTodayReadyCountMap = new HashMap<>();
        if( ! scrapQueueDailies.isEmpty()) {
            accountTodayReadyCountMap = scrapQueueDailies.stream().collect(Collectors.toMap(ScrapQueueDaily::getAccountId, Function.identity()));
            log.info("accountTodayReadyCountMap : {}", toJson(accountTodayReadyCountMap));
        }

        // 일일 스크랩
        List<AccountInfo> accountInfos = accountInfoService.getAll();
        for (AccountInfo accountInfo : accountInfos) {

            final int beforeDailyCount = accountInfo.getDailyCount();

            log.info("[default] daily count is set to 0 ! account : {}, daily count : {} -> {}",
                toJson(accountInfo), beforeDailyCount, accountInfo.getDailyCount());

            accountInfo.setDailyCount(0);

            final ScrapQueueDaily scrapQueueDaily = accountTodayReadyCountMap.get(accountInfo.getId());
            if(scrapQueueDaily != null) {
                log.info("[reserved] daily count is set to 0 ! account : {}, daily count : {} -> {}, todayReadyCount: {},",
                    toJson(accountInfo), beforeDailyCount, accountInfo.getDailyCount(), scrapQueueDaily.getCount());

                // 그날에 대기인게 미리 있다면 대기숫자를 빼준다.
                accountInfo.setDailyCount(scrapQueueDaily.getCount());
            }

            accountInfoService.save(accountInfo);
        }
    }

    public void searchKeywordWithImage() {
        this.searchKeywordService.searchKeywordWithImage();
    }
}
