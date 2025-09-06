package com.tbm.admin.batch;

import com.tbm.admin.service.batch.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("batch-keyword")
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordSearchBatch {
    private final BatchService batchService;

    @Scheduled(cron = "0 * * * * *")
    public void searchKeywordWithImage() {
        batchService.searchKeywordWithImage();
    }
}
