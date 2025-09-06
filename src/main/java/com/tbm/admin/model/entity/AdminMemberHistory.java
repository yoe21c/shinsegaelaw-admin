package com.tbm.admin.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Entity
@Table
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(NON_NULL)
public class AdminMemberHistory {

    public AdminMemberHistory() { }

    public AdminMemberHistory(Long memberSeq, String action, String actionName, String description) {
        this.memberSeq = memberSeq;
        this.action = action;
        this.actionName = actionName;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long seq;

    private Long memberSeq;

    private String action;

    private String actionName;

    private String description;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

}