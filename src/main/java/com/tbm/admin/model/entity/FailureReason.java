package com.tbm.admin.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FailureReason {
    private Long scrapSeq;
    private String scrapUrl;
    private LocalDateTime tryAt;
    private String reason;
}
