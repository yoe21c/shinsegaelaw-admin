package com.tbm.admin.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class RepairAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private String blogId;

    private String status;  // active, inactive

    private String macAddress;

    private String ipAddress;

    private String description;

    private LocalDateTime pingAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Transient
    public String getPingStatus() {
        return pingAt != null && pingAt.isAfter(LocalDateTime.now().minusMinutes(11)) ? "ON" : "OFF";
    }
}
