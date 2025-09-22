package com.shinsegaelaw.admin.model.param.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shinsegaelaw.admin.model.enums.Ec2State;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ec2Instance {
    private String id;
    private String name;
    private String ipAddress;
    private Ec2State state;
    private CpuUsage lastCpuUsage;
    private LocalDateTime launchTime;

    @Getter
    @AllArgsConstructor @Builder
    public static class CpuUsage {
        private Double maximum;
        private String unit;
        private LocalDateTime timestamp;
    }
}