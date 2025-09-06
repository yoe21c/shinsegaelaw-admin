package com.tbm.admin.interceptor;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        if (request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper")) return;

        if(!(request instanceof ContentCachingRequestWrapper)) {
            return;
        }

        final ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
        final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;

        // 요청이 json 인 경우
        if (cachingRequest.getContentType() != null && cachingRequest.getContentType().contains("application/json") && cachingRequest.getContentAsByteArray().length != 0) {
            try{
                final JsonNode jsonNode = objectMapper.readTree(cachingRequest.getContentAsByteArray());
                log.debug("Request Body : {}", jsonNode);
            }catch (JacksonException e) {
                log.warn("Request Body : {}", new String(cachingRequest.getContentAsByteArray()), e);
            }
        }

        // 요청이 json 이 아닌 경우
        else if (cachingRequest.getContentType() != null && cachingRequest.getContentType().contains("application/x-www-form-urlencoded") && cachingRequest.getContentAsByteArray().length != 0) {
            try{
                log.debug("Request Body : {}", new String(cachingRequest.getContentAsByteArray()));
            }catch (Exception e) {
                log.warn("Request Body : {}", new String(cachingRequest.getContentAsByteArray()), e);
            }
        }

        if (cachingResponse.getContentType() != null && cachingResponse.getContentType().contains("application/json") && cachingResponse.getContentAsByteArray().length != 0) {
            log.debug("Response Body : {}", objectMapper.readTree(cachingResponse.getContentAsByteArray()));
        }
    }
}