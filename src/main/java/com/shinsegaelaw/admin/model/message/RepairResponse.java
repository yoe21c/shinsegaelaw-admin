package com.shinsegaelaw.admin.model.message;

import lombok.Data;

@Data
public class RepairResponse {
    private String result;
    private String message;
    private Long repairAgentQueueSeq;
    private String repairType;
}