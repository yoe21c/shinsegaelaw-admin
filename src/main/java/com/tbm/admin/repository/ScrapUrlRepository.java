package com.tbm.admin.repository;

import com.tbm.admin.model.entity.ScrapUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScrapUrlRepository extends BaseRepository<ScrapUrl, Long> {

    @Query(value = """
        select * from ScrapUrl
        where seq is not null
         and (:status is null or status = :status)
         and (:memberSeq is null or memberSeq = :memberSeq)
         and (:keyword is null or (blogUrl like concat('%', :keyword, '%')))
    """, countProjection = "seq", nativeQuery = true)
    Page<ScrapUrl> findAllWith(String status, String keyword, Long memberSeq, Pageable pageable);

    Optional<ScrapUrl> findByBlogUrl(String url);

    List<ScrapUrl> findByStatusAndStartAtBeforeAndEndAtAfter(String status, LocalDateTime now1, LocalDateTime now2);

    List<ScrapUrl> findByStatusAndEndAtBefore(String status, LocalDateTime now);
}
