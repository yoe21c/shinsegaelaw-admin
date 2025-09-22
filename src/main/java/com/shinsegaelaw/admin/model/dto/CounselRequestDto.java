package com.shinsegaelaw.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounselRequestDto {

    private String id; // 보통 핸드폰 번호

    private String url; // S3 full URL 값
}