package com.shinsegaelaw.admin.service.thirdparty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinsegaelaw.admin.model.dto.OllamaResponseDto;
import com.shinsegaelaw.admin.model.dto.WhisperResponseDto;
import com.shinsegaelaw.admin.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.model:gpt-oss:20b}")
    private String ollamaModel;

    @Value("${ollama.api.timeout:180000}")
    private int ollamaApiTimeout;

    /**
     * Ollama 서버 연결 테스트
     */
    public boolean testConnection() {
        String tagsUrl = ollamaApiUrl + "/api/tags";

        try {
            log.info("Testing Ollama server connection at: {}", tagsUrl);
            ResponseEntity<Map> response = restTemplate.getForEntity(tagsUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("models")) {
                    List<Map<String, String>> models = (List<Map<String, String>>) body.get("models");
                    log.info("Ollama server connected. Available models: {}",
                            models.stream().map(m -> m.get("name")).toList());
                    return true;
                }
            }

            log.warn("Ollama server connection test failed");
            return false;

        } catch (Exception e) {
            log.error("Error connecting to Ollama server", e);
            return false;
        }
    }

    /**
     * 법률 상담 평가 실행
     */
    public Map<String, Object> evaluateConsultation(WhisperResponseDto conversationData) {
        log.info("Starting legal consultation evaluation");

        Map<String, Object> allResults = new HashMap<>();

        // 1. 종합 평가
        Map<String, Object> comprehensiveResult = performEvaluation(conversationData, "comprehensive");
        allResults.put("comprehensive_evaluation", comprehensiveResult);

        // 2. 수임 가능성 평가
        Map<String, Object> businessResult = performEvaluation(conversationData, "business_potential");
        allResults.put("business_potential", businessResult);

        // 3. 법률 전문성 평가
        Map<String, Object> expertiseResult = performEvaluation(conversationData, "expertise");
        allResults.put("expertise", expertiseResult);

        // 4. 의사소통 평가
        Map<String, Object> communicationResult = performEvaluation(conversationData, "communication");
        allResults.put("communication", communicationResult);

        // 5. 친절도 평가
        Map<String, Object> friendlinessResult = performEvaluation(conversationData, "friendliness");
        allResults.put("friendliness", friendlinessResult);

        log.info("Completed all evaluations");
        return allResults;
    }

    /**
     * 특정 유형의 평가 수행
     */
    private Map<String, Object> performEvaluation(WhisperResponseDto conversationData, String evaluationType) {
        log.info("Performing {} evaluation", evaluationType);

        String prompt = createPrompt(conversationData, evaluationType);
        int maxRetries = 3;

        for (int retry = 0; retry < maxRetries; retry++) {
            if (retry > 0) {
                log.info("Retry attempt {} for {} evaluation", retry, evaluationType);
                try {
                    Thread.sleep(3000); // 3초 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            OllamaResponseDto response = callOllama(prompt, retry);
            if (response != null && response.getResponse() != null) {
                Map<String, Object> parsed = parseEvaluationResponse(response.getResponse());
                if (parsed != null) {
                    log.info("Successfully completed {} evaluation", evaluationType);
                    return parsed;
                }
            }
        }

        log.error("Failed to complete {} evaluation after {} retries", evaluationType, maxRetries);
        return Map.of("error", "Evaluation failed after " + maxRetries + " retries");
    }

    /**
     * 평가 프롬프트 생성
     */
    private String createPrompt(WhisperResponseDto conversationData, String evaluationType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("중요: 반드시 유효한 JSON 형식으로만 응답하세요. 설명이나 주석 없이 JSON만 제공하세요.\n");
        prompt.append("⚠️ 매우 중요: 모든 텍스트 값은 반드시 한글로 작성하세요.\n\n");

        switch (evaluationType) {
            case "business_potential":
                prompt.append(getBusinessPotentialPrompt());
                break;
            case "expertise":
                prompt.append(getExpertisePrompt());
                break;
            case "communication":
                prompt.append(getCommunicationPrompt());
                break;
            case "friendliness":
                prompt.append(getFriendlinessPrompt());
                break;
            default: // comprehensive
                prompt.append(getComprehensivePrompt());
        }

        prompt.append("\n\n대화 데이터:\n");
        prompt.append(Utils.toJson(conversationData.getData().getSegments()));

        return prompt.toString();
    }

    /**
     * Ollama API 호출
     */
    private OllamaResponseDto callOllama(String prompt, int retryCount) {
        String generateUrl = ollamaApiUrl + "/api/generate";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", ollamaModel);
        payload.put("prompt", prompt);
        payload.put("stream", false);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", retryCount > 0 ? 0.1 : 0.2);
        options.put("top_p", 0.9);
        options.put("num_predict", 8000);
        options.put("seed", 42 + retryCount);
        payload.put("options", options);

        try {
            log.debug("Calling Ollama API with model: {}", ollamaModel);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<OllamaResponseDto> response = restTemplate.postForEntity(
                generateUrl,
                request,
                OllamaResponseDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Ollama API call successful");
                return response.getBody();
            }

            log.error("Ollama API returned non-OK status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error calling Ollama API", e);
            return null;
        }
    }

    /**
     * Ollama 응답 파싱
     */
    private Map<String, Object> parseEvaluationResponse(String responseText) {
        if (responseText == null || responseText.isEmpty()) {
            return null;
        }

        // JSON 코드 블록 제거
        String cleanText = responseText.trim();
        if (cleanText.startsWith("```json")) {
            cleanText = cleanText.substring(7);
        }
        if (cleanText.startsWith("```")) {
            cleanText = cleanText.substring(3);
        }
        if (cleanText.endsWith("```")) {
            cleanText = cleanText.substring(0, cleanText.length() - 3);
        }
        cleanText = cleanText.trim();

        try {
            JsonNode jsonNode = objectMapper.readTree(cleanText);

            // evaluation 키가 있는지 확인
            if (jsonNode.has("evaluation")) {
                return objectMapper.convertValue(jsonNode.get("evaluation"), Map.class);
            } else {
                return objectMapper.convertValue(jsonNode, Map.class);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse evaluation response", e);

            // 잘린 JSON 복구 시도
            String fixedJson = attemptJsonRecovery(cleanText);
            if (fixedJson != null) {
                try {
                    JsonNode fixedNode = objectMapper.readTree(fixedJson);
                    log.info("Successfully recovered truncated JSON");

                    if (fixedNode.has("evaluation")) {
                        return objectMapper.convertValue(fixedNode.get("evaluation"), Map.class);
                    } else {
                        return objectMapper.convertValue(fixedNode, Map.class);
                    }
                } catch (JsonProcessingException e2) {
                    log.error("Failed to parse recovered JSON", e2);
                }
            }

            return null;
        }
    }

    /**
     * 잘린 JSON 복구 시도
     */
    private String attemptJsonRecovery(String text) {
        // 괄호 수 계산
        int openBraces = 0;
        int closeBraces = 0;
        int openBrackets = 0;
        int closeBrackets = 0;

        for (char c : text.toCharArray()) {
            switch (c) {
                case '{': openBraces++; break;
                case '}': closeBraces++; break;
                case '[': openBrackets++; break;
                case ']': closeBrackets++; break;
            }
        }

        // 부족한 닫는 괄호 추가
        StringBuilder fixed = new StringBuilder(text);

        while (openBrackets > closeBrackets) {
            fixed.append(']');
            closeBrackets++;
        }

        while (openBraces > closeBraces) {
            fixed.append('}');
            closeBraces++;
        }

        // 마지막 콤마 제거
        String result = fixed.toString().replaceAll(",\\s*([\\]}])", "$1");

        return result;
    }

    // 각 평가 유형별 프롬프트
    private String getBusinessPotentialPrompt() {
        return """
            당신은 대형 로펌의 대표입니다. 다음 상담 내용을 분석하여 수임 가능성과 매출 기여도를 평가해주세요.

            평가 기준:
            1. 고객의 사건 규모와 복잡성
            2. 고객의 지불 능력 파악 여부
            3. 수임 가능성을 높이는 상담사의 전략
            4. 고객의 긴급성과 필요성 파악
            5. 경쟁 로펌 대비 차별화 포인트 제시

            필수 JSON 구조:
            {
              "evaluation": {
                "business_score": [0-100 점수],
                "potential_revenue": "[예상 수임료 범위]",
                "conversion_probability": [0-100 퍼센트],
                "recommendations": "[수임 전략 제안 - 한글로 작성]"
              }
            }
            """;
    }

    private String getExpertisePrompt() {
        return """
            당신은 법률 전문가입니다. 상담사의 법률 전문성을 엄격하게 평가해주세요.

            평가 기준:
            1. 법률 용어 사용의 정확성
            2. 관련 법령 및 판례 인용 여부
            3. 법적 쟁점 파악 능력
            4. 해결 방안의 구체성과 실현 가능성

            필수 JSON 구조:
            {
              "evaluation": {
                "expertise_score": [0-100 점수],
                "expertise_gaps": "[보완 필요 영역]",
                "training_needs": "[교육 필요사항 - 한글로 작성]"
              }
            }
            """;
    }

    private String getCommunicationPrompt() {
        return """
            커뮤니케이션 전문가 관점에서 상담사의 의사소통 명확성을 평가해주세요.

            평가 기준:
            1. 복잡한 법률 내용의 쉬운 설명
            2. 고객 질문에 대한 직접적 답변
            3. 정보의 구조화와 논리적 전달
            4. 고객 이해도 확인 여부

            필수 JSON 구조:
            {
              "evaluation": {
                "communication_score": [0-100 점수],
                "missed_clarifications": "[놓친 설명 기회]",
                "communication_improvements": "[개선 방안 - 한글로 작성]"
              }
            }
            """;
    }

    private String getFriendlinessPrompt() {
        return """
            고객 서비스 관점에서 상담사의 친절도와 관계 구축 능력을 평가해주세요.

            평가 기준:
            1. 공감과 이해 표현
            2. 고객 감정 상태 파악과 대응
            3. 적극적 경청 자세
            4. 신뢰감 형성 노력

            필수 JSON 구조:
            {
              "evaluation": {
                "friendliness_score": [0-100 점수],
                "relationship_potential": "[장기 관계 가능성 평가]",
                "customer_retention_likelihood": [0-100]
              }
            }
            """;
    }

    private String getComprehensivePrompt() {
        return """
            로펌 대표 관점에서 전체적인 상담 품질을 종합 평가해주세요.

            필수 JSON 구조:
            {
              "evaluation": {
                "total_score": [0-100],
                "summary_scores": {
                  "business_potential": [0-100],
                  "legal_expertise": [0-100],
                  "communication_clarity": [0-100],
                  "customer_friendliness": [0-100]
                },
                "executive_summary": "[대표 보고용 핵심 요약]",
                "strengths": ["[강점1]", "[강점2]"],
                "weaknesses": ["[약점1]", "[약점2]"],
                "action_items": ["[조치사항1]", "[조치사항2]"],
                "consultant_rating": "[S/A/B/C/D 등급]"
              }
            }
            """;
    }
}