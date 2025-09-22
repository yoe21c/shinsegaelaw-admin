package com.shinsegaelaw.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounselResponseDto {

    private boolean success;
    private String message;
    private Long counselId;
    private String status;
}