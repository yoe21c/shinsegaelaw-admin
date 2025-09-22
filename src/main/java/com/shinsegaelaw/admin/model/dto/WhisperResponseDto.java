package com.shinsegaelaw.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhisperResponseDto {

    private String result;
    private Long counselId;
    private String message;
    private String audioFile;
    private Double duration;
    private Double processingTime;
    private List<Segment> segments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        private String speaker;
        private Double start;
        private Double end;
        private String text;
    }
}