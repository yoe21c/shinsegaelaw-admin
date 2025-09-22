package com.shinsegaelaw.admin.service.thirdparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinsegaelaw.admin.model.dto.WhisperResponseDto;
import com.shinsegaelaw.admin.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.shinsegaelaw.admin.utils.Utils.toJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${whisper.api.url:http://3.38.59.217:5000}")
    private String whisperApiUrl;

    @Value("${whisper.api.timeout:180000}")
    private int whisperApiTimeout;

    /**
     * Whisper API 서버 헬스체크
     */
    public boolean checkHealth() {
        String healthCheckUrl = whisperApiUrl + "/api/health";

        try {
            log.info("Checking Whisper API health at: {}", healthCheckUrl);
            ResponseEntity<Map> response = restTemplate.getForEntity(healthCheckUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                if (body != null && "ok".equals(body.get("result"))) {
                    log.info("Whisper API health check successful");
                    return true;
                }
            }

            log.warn("Whisper API health check failed with response: {}", response);
            return false;

        } catch (Exception e) {
            log.error("Error checking Whisper API health", e);
            return false;
        }
    }

    /**
     * Whisper API 호출하여 음성 파일 처리
     */
    public String processAudioFile(String url) {
        String processUrl = whisperApiUrl + "/api/counsel/add";

        try {
            log.info("Processing audio file with Whisper API: {}", url);

            // 요청 본문 생성
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("url", url);

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            // API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(
                processUrl,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();

                if (body != null && "ok".equals(body.get("result"))) {
                    log.info("Whisper processing initiated successfully for counselId: {}",
                            body.get("counsel_id"));

                    // 처리가 시작되었으므로, 일정 시간 대기 후 결과 조회
                    return Utils.toJson(response.getBody());
                }
            }

            log.error("Failed to process audio with Whisper API: {}", response);
            throw new RuntimeException("Whisper API processing failed");

        } catch (Exception e) {
            log.error("Error processing audio with Whisper API", e);
            throw new RuntimeException("Failed to process audio file", e);
        }
    }

}