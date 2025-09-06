package com.tbm.admin.repository;

import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.view.base.ScrapQueueDaily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScrapQueueRepository extends BaseRepository<ScrapQueue, Long> {

    @Query(value = """
       select queue.*,
              :role as role
       from ScrapQueue queue
           join AccountInfo account ON queue.accountSeq = account.seq
           join ScrapUrl url ON queue.urlSeq = url.seq
       where queue.seq is not null
         and (:status is null or queue.status = :status)
         and (:keyword is null or (account.id like concat('%', :keyword, '%') or url.blogUrl like concat('%', :keyword, '%')))
         and (:role = 'superadmin' or queue.memberSeq = :memberSeq)
    """, countQuery = """
       select count(*)
       from ScrapQueue queue
           join AccountInfo account ON queue.accountSeq = account.seq
           join ScrapUrl url ON queue.urlSeq = url.seq
       where queue.seq is not null
         and (:status is null or queue.status = :status)
         and (:keyword is null or (account.id like concat('%', :keyword, '%') or url.blogUrl like concat('%', :keyword, '%')))
         and (:role = 'superadmin' or queue.memberSeq = :memberSeq)
    """, nativeQuery = true)
    Page<ScrapQueue> findAllWith(String status, String keyword, Long memberSeq, String role, Pageable pageable);

    List<ScrapQueue> findByUrlSeq(Long urlSeq);

    @Query(value = """
        select * from ScrapQueue 
        where accountSeq = :accountSeq 
         and status = :status 
         and startAt < now() 
         and date(startAt) = date(now()) 
        ORDER BY startAt 
     """, nativeQuery = true)
    List<ScrapQueue> findAllScrapQueuesWith(Long accountSeq, String status);

    @Query(value = """
        select accountId, date(startAt) date, count(*) count
        from ScrapQueue
        where status = 'ready'
         and date(startAt) = date(now())
        group by accountId
     """, nativeQuery = true)
    List<ScrapQueueDaily> findReadyTodayAll();

    // todo update 구문으로 바로 processing 으로 수정한다.
}
