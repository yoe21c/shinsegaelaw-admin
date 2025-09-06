package com.tbm.admin.service.persist;

import com.tbm.admin.model.entity.FeedHistory;
import com.tbm.admin.model.entity.FeedUrl;
import com.tbm.admin.repository.FeedHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedHistoryService {

    private final FeedHistoryRepository feedHistoryRepository;

    public void save(FeedHistory feedHistory) {
        feedHistoryRepository.save(feedHistory);
    }

    public boolean existFeedHistory(FeedUrl feedUrl, String logNo) {
        return feedHistoryRepository.existFeedHistory(feedUrl, logNo);
    }

    public void saveAll(LinkedHashSet<FeedHistory> feedHistories) {
        feedHistoryRepository.saveAll(feedHistories);
    }

    public List<FeedHistory> findFeedHistoryByCreatedAtBetween3Hours() {
        return feedHistoryRepository.findFeedHistoryByCreatedAtBetween3Hours();
    }


}
