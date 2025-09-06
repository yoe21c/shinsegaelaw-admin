package com.tbm.admin.repository;

import com.tbm.admin.model.entity.AccountInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountInfoRepository extends BaseRepository<AccountInfo, Long> {

    // 에이전트 할당
    @Query(value = """
        select * from AccountInfo
        where seq is not null 
         and status = 'active'
         and ipAddress is not null
         and (id like concat('%', :keyword, '%') 
            or ipAddress like concat('%', :keyword, '%'))
    """, countProjection = "seq", nativeQuery = true)
    Page<AccountInfo> findAllWithoutUnassignedIp(String keyword, Pageable pageable);

    // 에이전트 미할당
    @Query(value = """
        select * from AccountInfo
        where seq is not null
         and status = 'active'
         and ipAddress is null
         and id like concat('%', :keyword, '%')
    """, countProjection = "seq", nativeQuery = true)
    Page<AccountInfo> findAllWithoutAssignedIP(String keyword, Pageable pageable);

    //전체
    @Query(value = """
        select * from AccountInfo
        where seq is not null 
          and status = 'active'
          and (id like concat('%', :keyword, '%') 
            or ipAddress like concat('%', :keyword, '%'))
    """, countProjection = "seq", nativeQuery = true)
    Page<AccountInfo> findAllWith(String keyword, Pageable pageable);

    Optional<AccountInfo> findByIpAddress(String ipAddress);

    Optional<AccountInfo> findFirstByStatusAndIpAddressIsNullAndIpAddressIsNot(String status, String ipAddress);

    Optional<AccountInfo> findFirstByStatusAndIdIsNotNullAndIpAddressIsNull(String status);

    int countAccountInfoByIpAddressNotNull();

    /**
     * 데일리 스크랩 카운트가 100 회를 넘지 않는 에이전트가 유효한 에이전트이다.
     * @return
     */
    @Query(value = """
        select * from AccountInfo
        where seq is not null
         and status = 'active'
         and ipAddress is not null
         and dailyCount < dailyCountLimit
        order by dailyCount
    """, nativeQuery = true)
    List<AccountInfo> findAccountInfos();

    List<AccountInfo> findAllByStatusAndDailyCountLessThan(String status, int limitDailyCount);

    Optional<AccountInfo> findByInstanceId(String instanceId);
}
