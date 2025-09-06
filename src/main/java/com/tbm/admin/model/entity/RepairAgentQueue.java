package com.tbm.admin.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static com.tbm.admin.utils.DateUtils.getTimeDifference;

@Getter @Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class RepairAgentQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private Long agentSeq;

    private String status;  // reserved, processing, completed, failed, deleted

    private String macAddress;

    private String ipAddress;

    private String searchText;

    private String blogId;

    private String blogUrl;

    private String result;

    private String title;

    private String content;

    private Integer lineNumber;

    private String failReason;

    private LocalDateTime reservedAt;
    private LocalDateTime processingAt;
    private LocalDateTime completedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime failedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Transient
    private String currentTitle;

    // added
    @Transient
    public String getReservedAtDiff() {
        return getTimeDifference(reservedAt);
    }

    @Transient
    public boolean isDeleted() {
        return getStatus().equals("deleted");
    }

    @Transient
    public String getDisplayStatusName() {
        switch (status) {
            case "reserved":
                return "예약";
            case "processing":
                return "처리중";
            case "completed":
                return "완료";
            case "failed":
                return "실패";
            case "deleted":
                return "삭제";
            default:
                return "알수없음";
        }
    }
}
