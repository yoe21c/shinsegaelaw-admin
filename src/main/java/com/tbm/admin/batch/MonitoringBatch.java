package com.tbm.admin.batch;

import com.tbm.admin.service.persist.FeedHistoryService;
import com.tbm.admin.service.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("monitoring")
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringBatch {

    private final FeedHistoryService feedHistoryService;
    private final TelegramService telegramService;

    // 60초 마다
    @Scheduled(fixedDelay = 60000L)
    public void updateScrapUrlStatus() {
        if(feedHistoryService.findFeedHistoryByCreatedAtBetween3Hours().isEmpty()) {
            log.info("No feed history data for 3 hours");
            telegramService.sendTelegram("No feed history data for 3 hours !!");
        }
    }

}
