package com.tbm.admin.service.persist;

import com.tbm.admin.model.entity.FeedUrl;
import com.tbm.admin.repository.FeedUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedUrlService {

    private final FeedUrlRepository feedUrlRepository;

    public List<FeedUrl> getActiveTargetFeedUrls() {
        return feedUrlRepository.findAllByActiveAndMonitoringCountGreaterThan("Y", 0);
    }

    public Page<FeedUrl> getAllWith(String keyword, Long memberSeq, PageRequest pageable) {
        return feedUrlRepository.findAllByBlogUrl(keyword, memberSeq, pageable);
    }

    public FeedUrl getFeedUrl(Long seq) {
        return feedUrlRepository.findById(seq)
                .orElseThrow(() -> new RuntimeException("[Admin] Not Exist url seq : " + seq));
    }

    public void save(FeedUrl feedUrl) {
        feedUrlRepository.save(feedUrl);
    }

    public FeedUrl getFeedUrlByBlogUrl(String blogUrl) {
        return feedUrlRepository.findByBlogUrl(blogUrl).orElse(null);
    }

    public void deleteFeedUrl(Long seq) {
        feedUrlRepository.deleteById(seq);
    }
}
