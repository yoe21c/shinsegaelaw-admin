package com.shinsegaelaw.admin.batch;

import com.shinsegaelaw.admin.model.entity.Counsel;
import com.shinsegaelaw.admin.service.persist.CounselService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CounselProcessing {

    private final CounselService counselService;

    @Scheduled(fixedDelayString = "10000") // Runs every 10 seconds
    public void processCounselRequests() {
        // Logic to process counsel requests in batch
//        log.debug("Processing counsel requests in batch...");

        final List<Counsel> candidates = counselService.getCandidates();

        if(candidates != null && candidates.size() > 0) {
            log.info("Found {} counsel requests to process.", candidates.size());
            for (Counsel counsel : candidates) {
                counselService.processAsync(counsel);
            }
        }

    }
}
