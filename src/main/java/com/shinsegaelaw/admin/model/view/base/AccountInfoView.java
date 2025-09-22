package com.shinsegaelaw.admin.model.view.base;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountInfoView {

    private Long seq;
    private String assignment;
    private String id;
    private String password;
    private String ipAddress;
    private int dailyCount;
    private int dailyCountLimit;
    private LocalDateTime createdAt;

}
