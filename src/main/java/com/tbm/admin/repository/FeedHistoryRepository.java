package com.tbm.admin.repository;

import com.tbm.admin.model.entity.FeedHistory;
import com.tbm.admin.model.entity.FeedUrl;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FeedHistoryRepository extends BaseRepository<FeedHistory, Long> {

    @Query("SELECT exists (SELECT f FROM FeedHistory f WHERE f.feedUrl = :feedUrl AND f.logNo = :logNo)")
    boolean existFeedHistory(FeedUrl feedUrl, String logNo);

    @Query(value = "SELECT * FROM FeedHistory WHERE createdAt BETWEEN DATE_SUB(NOW(), INTERVAL 3 HOUR) AND NOW() AND HOUR(createdAt) BETWEEN 9 AND 23", nativeQuery = true)
    List<FeedHistory> findFeedHistoryByCreatedAtBetween3Hours();
}
