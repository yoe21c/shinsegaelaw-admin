package com.tbm.admin.service.persist;

import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.view.base.ScrapQueueDaily;
import com.tbm.admin.repository.ScrapQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapQueueService {

    private final ScrapQueueRepository scrapQueueRepository;

    public Page<ScrapQueue> findAllWith(String status, String keyword, Long memberSeq, String role, Pageable pageable) {
        return scrapQueueRepository.findAllWith(status, keyword, memberSeq, role, pageable);
    }

    public List<ScrapQueue> getAllReadyByAccountSeq(Long accountSeq) {
        return scrapQueueRepository.findAllScrapQueuesWith(accountSeq, "ready");
    }

    public List<ScrapQueueDaily> getReadyTodayAll() {
        return scrapQueueRepository.findReadyTodayAll();
    }

    public void save(ScrapQueue scrapQueue) {
        scrapQueueRepository.save(scrapQueue);
    }

    public Optional<ScrapQueue> getScrapQueueBySeq(Long seq) {
        return scrapQueueRepository.findById(seq);
    }

}
