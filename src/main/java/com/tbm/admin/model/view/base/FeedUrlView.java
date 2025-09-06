package com.tbm.admin.model.view.base;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedUrlView {

    private Long seq;
    private Long memberSeq;
    private String blogUrl;
    private LocalDateTime reservedAt;
    private int targetScrapCount = 5;
    private int monitoringCount = 30;

}
