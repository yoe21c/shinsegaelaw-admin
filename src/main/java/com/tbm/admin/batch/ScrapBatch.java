package com.tbm.admin.batch;

import com.tbm.admin.service.batch.BatchService;
import com.tbm.admin.service.scrap.ScrapAgentRequester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("batch")
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapBatch {
    private final BatchService batchService;
    private final ScrapAgentRequester scrapAgentRequester;

    // agent-url 매핑된 scrapQueue 생성 - 5초마다 ===> 이 로직은 필요없음. URL 기입할 때 생성하도록 수정.
//    @Scheduled(cron = "0/5 * * * * *")
//    public void createScrapQueue() { batchService.createScrapQueue(); }

    // url 예약 상태 변경 - 1초마다
    @Scheduled(cron = "0/1 * * * * *")
    public void updateScrapUrlStatus() {
//        batchService.updateScrapUrlStatus();
    }

    // 일일 스크랩 개수 초기화 - 1일마다
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyCount() {
        batchService.resetDailyCount();
    }

    // 1초마다 scrap 메소드 실행
    @Scheduled(fixedDelay = 1000L)
    public void scrap() {
        scrapAgentRequester.requestScrap();
    }
}
