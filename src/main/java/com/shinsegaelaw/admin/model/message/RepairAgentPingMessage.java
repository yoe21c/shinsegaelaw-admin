package com.shinsegaelaw.admin.model.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class RepairAgentPingMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ip;
    private String mac;
}