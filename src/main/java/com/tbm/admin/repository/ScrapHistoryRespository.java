package com.tbm.admin.repository;

import com.tbm.admin.model.entity.ScrapHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface ScrapHistoryRespository extends BaseRepository<ScrapHistory, Long> {

    @Query(value = """
        select * from ScrapHistory
        where seq is not null
         and result != 'N'
         and (accountId like concat('%', :keyword, '%') 
                or blogUrl like concat('%', :keyword, '%'))
         and (:memberSeq is null or memberSeq = :memberSeq)
    """, countProjection = "seq", nativeQuery = true)
    Page<ScrapHistory> findAllWithoutFail(String keyword, Long memberSeq, Pageable pageable);

    @Query(value = """
        select * from ScrapHistory
        where seq is not null
         and result != 'Y'
         and (accountId like concat('%', :keyword, '%') 
                or blogUrl like concat('%', :keyword, '%'))
         and (:memberSeq is null or memberSeq = :memberSeq)
    """, countProjection = "seq", nativeQuery = true)
    Page<ScrapHistory> findAllWithoutSuccess(String keyword, Long memberSeq, Pageable pageable);

    @Query(value = """
        select * from ScrapHistory
        where seq is not null 
        and (accountId like concat('%', :keyword, '%') 
                or blogUrl like concat('%', :keyword, '%'))
        and (:memberSeq is null or memberSeq = :memberSeq)
    """, countProjection = "seq", nativeQuery = true)
    Page<ScrapHistory> findAllWith(String keyword, Long memberSeq, Pageable pageable);

}
