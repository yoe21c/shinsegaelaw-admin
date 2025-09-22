package com.shinsegaelaw.admin.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhisperResponseDto {

    private String result;
    private WhisperData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhisperData {
        private Double duration;
        @JsonProperty("duration_ms")
        private Long durationMs;
        @JsonProperty("processing_time")
        private Double processingTime;
        private List<Segment> segments;
        @JsonProperty("full_transcript")
        private String fullTranscript;
        @JsonProperty("speaker_transcripts")
        private Map<String, String> speakerTranscripts;
        private Statistics statistics;
        private Metadata metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        private String speaker;
        private Double start;
        private Double end;
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        @JsonProperty("total_processing_time")
        private Double totalProcessingTime;
        @JsonProperty("audio_duration")
        private Double audioDuration;
        @JsonProperty("processing_speed")
        private Double processingSpeed;
        @JsonProperty("num_segments")
        private Integer numSegments;
        @JsonProperty("num_speakers")
        private Integer numSpeakers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String url;
        private String customer;
        private String customerPhoneNumber;
        private String counselAt;
    }
}