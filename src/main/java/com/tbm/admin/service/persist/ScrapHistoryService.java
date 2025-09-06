package com.tbm.admin.service.persist;

import com.tbm.admin.model.entity.ScrapHistory;
import com.tbm.admin.repository.ScrapHistoryRespository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapHistoryService {

    private final ScrapHistoryRespository scrapHistoryRespository;

    // 스크랩 성공
    public Page<ScrapHistory> getAllWithoutFail(String keyword, Long memberSeq, Pageable pageable) {
        return scrapHistoryRespository.findAllWithoutFail(keyword, memberSeq, pageable);
    }

    // 스크랩 실패
    public Page<ScrapHistory> getAllWithoutSuccess(String keyword, Long memberSeq, Pageable pageable) {
        return scrapHistoryRespository.findAllWithoutSuccess(keyword, memberSeq, pageable);
    }

    // 전체
    public Page<ScrapHistory> getAllWith(String keyword, Long memberSeq, Pageable pageable) {
        return scrapHistoryRespository.findAllWith(keyword, memberSeq, pageable);
    }


}
