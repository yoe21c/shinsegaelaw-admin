package com.shinsegaelaw.admin.model.view.base;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberHistoryView {

    // memberHistory 속성들
    private Long seq;
    private Long memberSeq;
    private String name;
    private String email;
    private String action;
    private String actionName;
    private String description;
    private LocalDateTime createdAt;
}
