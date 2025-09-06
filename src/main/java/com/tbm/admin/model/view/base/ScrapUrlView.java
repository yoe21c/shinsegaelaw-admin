package com.tbm.admin.model.view.base;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScrapUrlView {

    private Long seq;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String blogUrl;
    private String activate;
    private int count;
    private int targetCount;
    private LocalDateTime createdAt;

}
