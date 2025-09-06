package com.tbm.admin.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@EntityListeners(AuditingEntityListener.class)
public class ScrapUrl {

    public ScrapUrl() { }

    public ScrapUrl(String blogUrl, int targetCount) {
        this.blogUrl = blogUrl;
        this.targetCount = targetCount;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long seq;

    private Long memberSeq;

    @ColumnDefault("ready")
    private String status = "ready";  // ready, processing, closed

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String blogUrl;

    @ColumnDefault("Y")
    private String activate = "Y";

    @ColumnDefault("0")
    private int count = 0;

    private int targetCount;

    @Transient
    private boolean createdUrl;

    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime closedAt;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;
}
