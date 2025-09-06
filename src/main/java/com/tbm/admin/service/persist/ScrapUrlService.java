package com.tbm.admin.service.persist;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.ScrapUrl;
import com.tbm.admin.repository.ScrapUrlRepository;
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
public class ScrapUrlService {

    private final ScrapUrlRepository scrapUrlRepository;

    public Page<ScrapUrl> getAllWith(String status, String keyword, Long memberSeq, Pageable pageable) {
        return scrapUrlRepository.findAllWith(status, keyword, memberSeq, pageable);
    }

    public ScrapUrl findScrapUrl(Long seq) {
        return scrapUrlRepository.findById(seq)
                .orElseThrow(() -> new TbmAdminRuntimeException("[Admin] Not Exist url seq : " + seq));
    }

    public Optional<ScrapUrl> getScrapUrl(String url) {
        return scrapUrlRepository.findByBlogUrl(url);
    }

    public ScrapUrl getScrapUrl(Long seq) {
        if (seq == null) {
            ScrapUrl scrapUrl = new ScrapUrl();
            scrapUrl.setCreatedUrl(true);   // 신규생성임을 표시
            scrapUrl.setStatus("ready");
            scrapUrl.setCount(0);
            return scrapUrl;
        }
        return findScrapUrl(seq);
    }

    public ScrapUrl save(ScrapUrl scrapUrl) {
        return scrapUrlRepository.save(scrapUrl);
    }

    public void saveAll(List<ScrapUrl> scrapUrlList) {
        scrapUrlRepository.saveAll(scrapUrlList);
    }
}
