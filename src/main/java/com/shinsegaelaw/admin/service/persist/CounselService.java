package com.shinsegaelaw.admin.service.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinsegaelaw.admin.model.dto.CounselRequestDto;
import com.shinsegaelaw.admin.model.dto.CounselResponseDto;
import com.shinsegaelaw.admin.model.dto.WhisperResponseDto;
import com.shinsegaelaw.admin.model.entity.Counsel;
import com.shinsegaelaw.admin.repository.CounselRepository;
import com.shinsegaelaw.admin.service.thirdparty.OllamaService;
import com.shinsegaelaw.admin.service.thirdparty.WhisperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounselService {

    private final CounselRepository counselRepository;
    private final WhisperService whisperService;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    /**
     * 상담 처리 메인 로직
     */
    @Transactional
    public CounselResponseDto processCounsel(CounselRequestDto request) {
        log.info("Starting counsel processing for id: {}, url: {}", request.getId(), request.getUrl());

        try {
            // 1. URL에서 메타데이터 추출
            Map<String, Object> metadata = extractMetadata(request.getUrl());

            // 2. DB에 초기 데이터 저장
            Counsel counsel = createInitialCounsel(request, metadata);
            counsel = counselRepository.save(counsel);
            log.info("Created counsel record with seq: {}", counsel.getSeq());

            // 3. 비동기로 Whisper 및 Ollama 처리 시작
//            processAsync(counsel.getSeq(), request.getUrl());

            return CounselResponseDto.builder()
                    .success(true)
                    .message("Counsel processing initiated successfully")
                    .counselId(counsel.getSeq())
                    .status(counsel.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("Error processing counsel", e);
            throw new RuntimeException("Failed to process counsel", e);
        }
    }

    /**
     * URL에서 파일 메타데이터 추출
     */
    private Map<String, Object> extractMetadata(String url) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // URL에서 파일명 추출
            String[] urlParts = url.split("/");
            String encodedFileName = urlParts[urlParts.length - 1];
            String fileName = URLDecoder.decode(encodedFileName, StandardCharsets.UTF_8.name());

            metadata.put("fileName", fileName);

            // 파일 확장자 추출
            String fileType = "m4a"; // 기본값
            if (fileName.contains(".")) {
                String[] nameParts = fileName.split("\\.");
                fileType = nameParts[nameParts.length - 1].toLowerCase();
            }
            metadata.put("fileType", fileType);

            // 파일명에서 고객 정보 파싱
            parseFileNameMetadata(fileName, metadata);

            log.info("Extracted metadata from URL: {}", metadata);

        } catch (Exception e) {
            log.error("Error extracting metadata from URL: {}", url, e);
            // 기본값 설정
            metadata.put("fileName", "audio.m4a");
            metadata.put("fileType", "m4a");
        }

        return metadata;
    }

    /**
     * 파일명에서 고객 정보 추출
     * 포맷1: 고객전화번호_상담완료일시.m4a
     * 포맷2: 고객이름_고객전화번호_상담완료일시.m4a
     */
    private void parseFileNameMetadata(String fileName, Map<String, Object> metadata) {
        try {
            // 확장자 제거
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String[] parts = nameWithoutExt.split("_");

            if (parts.length == 2) {
                // 포맷1: 고객전화번호_상담완료일시
                metadata.put("customerPhoneNumber", parts[0]);
                parseCounselDateTime(parts[1], metadata);

            } else if (parts.length == 3) {
                // 포맷2: 고객이름_고객전화번호_상담완료일시
                metadata.put("customer", parts[0]);
                metadata.put("customerPhoneNumber", parts[1]);
                parseCounselDateTime(parts[2], metadata);

            } else {
                log.warn("Unexpected filename format: {}", fileName);
            }

        } catch (Exception e) {
            log.error("Error parsing filename metadata: {}", fileName, e);
        }
    }

    /**
     * 상담일시 문자열 파싱 (YYYYMMDDHHmmss 형식)
     */
    private void parseCounselDateTime(String dateTimeStr, Map<String, Object> metadata) {
        if (dateTimeStr != null && dateTimeStr.length() == 14 && dateTimeStr.matches("\\d+")) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                LocalDateTime counselAt = LocalDateTime.parse(dateTimeStr, formatter);
                metadata.put("counselAt", counselAt);
            } catch (Exception e) {
                log.warn("Failed to parse counsel datetime: {}", dateTimeStr, e);
            }
        }
    }

    /**
     * 초기 Counsel 엔티티 생성
     */
    private Counsel createInitialCounsel(CounselRequestDto request, Map<String, Object> metadata) {
        Counsel.CounselBuilder builder = Counsel.builder()
                .url(request.getUrl())
                .fileName((String) metadata.getOrDefault("fileName", "audio.m4a"))
                .fileType((String) metadata.getOrDefault("fileType", "m4a"))
                .status("ready");  // 초기 상태를 'ready'로 설정

        builder.counselorPhoneNumber(request.getId());

        // 선택적 필드 설정
        if (metadata.containsKey("customer")) {
            builder.customer((String) metadata.get("customer"));
        }

        if (metadata.containsKey("customerPhoneNumber")) {
            builder.customerPhoneNumber((String) metadata.get("customerPhoneNumber"));
        }

        if (metadata.containsKey("counselAt")) {
            builder.counselAt((LocalDateTime) metadata.get("counselAt"));
        }

        return builder.build();
    }

    /**
     * 비동기로 Whisper 및 Ollama 처리
     */
    @Async
    public void processAsync(Long counselSeq, String url) {
        log.info("Starting async processing for counsel seq: {}", counselSeq);

        try {
            // 1. 상태를 진행중으로 업데이트
            updateCounselStatus(counselSeq, "processing");

            // 2. Whisper API 헬스체크
            if (!whisperService.checkHealth()) {
                log.error("Whisper API is not healthy");
                updateCounselStatus(counselSeq, "error");
                return;
            }

            // 3. Whisper API 호출하여 음성 처리
            WhisperResponseDto whisperResponse = whisperService.processAudioFile(url);

            // 실제 Whisper API는 백그라운드로 처리되므로
            // 일정 시간 후 DB에서 결과를 폴링하여 확인
            String whisperJson = waitForWhisperResult(counselSeq);

            if (whisperJson != null) {
                // 4. Whisper 결과를 파싱하여 Ollama 평가용 데이터 준비
                Map<String, Object> conversationData = objectMapper.readValue(whisperJson, Map.class);

                // 5. Ollama API 호출하여 평가 실행
                if (ollamaService.testConnection()) {
                    Map<String, Object> evaluationResult = ollamaService.evaluateConsultation(conversationData);

                    // 6. 평가 결과에서 요약 추출 및 저장
                    updateCounselSummary(counselSeq, evaluationResult);

                    updateCounselStatus(counselSeq, "completed");
                    log.info("Successfully completed all processing for counsel seq: {}", counselSeq);
                } else {
                    log.error("Ollama server is not connected");
                    updateCounselStatus(counselSeq, "completed");  // Whisper만 완료된 경우도 완료로 표시
                }
            } else {
                log.error("Failed to get Whisper result for counsel seq: {}", counselSeq);
                updateCounselStatus(counselSeq, "error");
            }

        } catch (Exception e) {
            log.error("Error in async processing for counsel seq: {}", counselSeq, e);
            updateCounselStatus(counselSeq, "error");
        }
    }

    /**
     * Whisper 처리 결과 대기 및 조회
     */
    private String waitForWhisperResult(Long counselSeq) {
        int maxAttempts = 60; // 최대 5분 대기 (5초 * 60)
        int attempts = 0;

        while (attempts < maxAttempts) {
            try {
                Thread.sleep(5000); // 5초 대기

                Counsel counsel = counselRepository.findById(counselSeq).orElse(null);
                if (counsel != null && counsel.getWhisperJson() != null && !counsel.getWhisperJson().isEmpty()) {
                    log.info("Whisper result found for counsel seq: {}", counselSeq);
                    return counsel.getWhisperJson();
                }

                if ("완료".equals(counsel.getStatus()) || "extracted".equals(counsel.getStatus())) {
                    // Whisper 처리 완료 상태
                    return counsel.getWhisperJson();
                }

                attempts++;
                if (attempts % 12 == 0) { // 1분마다 로그
                    log.info("Still waiting for Whisper result... ({}m elapsed)", attempts / 12);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for Whisper result", e);
                return null;
            }
        }

        log.warn("Timeout waiting for Whisper result after {} attempts", maxAttempts);
        return null;
    }

    /**
     * 상담 상태 업데이트
     */
    @Transactional
    public void updateCounselStatus(Long counselSeq, String status) {
        counselRepository.findById(counselSeq).ifPresent(counsel -> {
            counsel.setStatus(status);
            counsel.setUpdatedAt(LocalDateTime.now());
            counselRepository.save(counsel);
            log.debug("Updated counsel seq: {} status to: {}", counselSeq, status);
        });
    }

    /**
     * Whisper 결과 저장
     */
    @Transactional
    public void updateCounselWhisperResult(Long counselSeq, String whisperJson) {
        counselRepository.findById(counselSeq).ifPresent(counsel -> {
            counsel.setWhisperJson(whisperJson);
            counsel.setWhisperAt(LocalDateTime.now());
            counsel.setUpdatedAt(LocalDateTime.now());

            // whisperJson에서 duration 추출하여 저장
            try {
                Map<String, Object> whisperData = objectMapper.readValue(whisperJson, Map.class);
                if (whisperData.containsKey("duration")) {
                    Double duration = (Double) whisperData.get("duration");
                    counsel.setFileDurationMs((int) (duration * 1000));
                }
            } catch (Exception e) {
                log.error("Error parsing whisper json for duration", e);
            }

            counselRepository.save(counsel);
            log.info("Updated Whisper result for counsel seq: {}", counselSeq);
        });
    }

    /**
     * 요약 결과 저장
     */
    @Transactional
    public void updateCounselSummary(Long counselSeq, Map<String, Object> evaluationResult) {
        counselRepository.findById(counselSeq).ifPresent(counsel -> {
            // 종합 평가에서 요약 추출
            Map<String, Object> comprehensive = (Map<String, Object>) evaluationResult.get("comprehensive_evaluation");
            if (comprehensive != null) {
                String summary = (String) comprehensive.get("executive_summary");
                if (summary != null && !summary.isEmpty()) {
                    // 100자 이내로 제한
                    if (summary.length() > 100) {
                        summary = summary.substring(0, 97) + "...";
                    }
                    counsel.setSummary(summary);
                }
            }

            counsel.setSummaryAt(LocalDateTime.now());
            counsel.setUpdatedAt(LocalDateTime.now());
            counselRepository.save(counsel);
            log.info("Updated summary for counsel seq: {}", counselSeq);
        });
    }

    /**
     * 상담 조회
     */
    @Transactional(readOnly = true)
    public Counsel getCounsel(Long counselSeq) {
        return counselRepository.findById(counselSeq)
                .orElseThrow(() -> new RuntimeException("Counsel not found: " + counselSeq));
    }

    /**
     * 전화번호로 상담 내역 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<Counsel> getCounselsByPhoneNumber(String phoneNumber) {
        return counselRepository.findByCustomerPhoneNumberOrderByCreatedAtDesc(phoneNumber);
    }
}