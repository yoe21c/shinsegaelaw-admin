package com.shinsegaelaw.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaResponseDto {

    private String model;
    private String createdAt;
    private String response;
    private Boolean done;
}