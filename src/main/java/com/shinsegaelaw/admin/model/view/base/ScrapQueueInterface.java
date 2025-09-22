package com.shinsegaelaw.admin.model.view.base;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public interface ScrapQueueInterface {

    Long getSeq();
    String getId();
    String getScrapComplete();
    String getBlogUrl();
    LocalDateTime getCreatedAt();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    LocalDateTime getCompletedAt();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    LocalDateTime getStartAt();
    String getRole();

    default String getId2() {
        if(getRole() != null && getRole().equalsIgnoreCase("superadmin")) {
            return getId();
        }
        return "******";
    }
}
