package com.tbm.admin.model.view.base;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScrapHistoryView {

    private Long seq;
    private String accountId;
    private String blogUrl;
    private String result;
    private String description;
    private LocalDateTime createdAt;

}