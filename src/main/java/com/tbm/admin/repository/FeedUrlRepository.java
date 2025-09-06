package com.tbm.admin.repository;

import com.tbm.admin.model.entity.FeedUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FeedUrlRepository extends BaseRepository<FeedUrl, Long> {

    List<FeedUrl> findAllByActiveAndMonitoringCountGreaterThan(String active, int monitoringCount);

    @Query(value = """
        select f from FeedUrl f
        where (:blogUrl is null or f.blogUrl like concat('%', :blogUrl, '%'))
        and (:memberSeq is null or f.memberSeq = :memberSeq)
        and f.active = 'Y'
        """,
        countProjection = "seq")
    Page<FeedUrl> findAllByBlogUrl(String blogUrl, Long memberSeq, Pageable pageable);

    @Query("select f from FeedUrl f where f.blogUrl = :blogUrl and f.active = 'Y'")
    Optional<FeedUrl> findByBlogUrl(String blogUrl);
}
