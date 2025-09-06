package com.tbm.admin.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ScrapQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long seq;

    private Long memberSeq; // 누가 요청했나?

    private Long urlSeq;    // 어떤 URL을 스크랩할 것인가?
    private String blogUrl;

    private String status;  // ready, processing, completed, failed, deleted

    private Long accountSeq;
    private String accountId;
    private String accountNickname;
    private String ipAddress;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH-mm-ss", timezone = "Asia/Seoul")
    private LocalDateTime startAt;

    private LocalDateTime endAt;    // 스크랩이 끝난 시간으로써 startAt 후로 1일뒤로 설정한다.

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;

    /******************************************************
     * 결과부분
     *****************************************************/
    private int tryCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime completedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime failedAt;

    private String reasons;     // List<FailureReason> 의 json 으로 저장한다.

}
