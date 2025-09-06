package com.tbm.admin.service.telegram;

import com.tbm.admin.config.AsyncConfiguration;
import com.tbm.admin.config.properties.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService {

    private final Environment environment;
    private final TelegramProperties telegramProperties;

    @Async(AsyncConfiguration.TELEGRAM_EXECUTOR)
    public void sendTelegram(String message) {
        message = "[" + getActiveFirstProfile() + "] " + message;
        try {
            send(message);
            log.info("[TODO: 실제전송시켜야함 ! ] message: {}", message);
        }catch (Exception e) {
            log.error("텔레그램 장애 상황.", e);
        }
    }

    private void send(String message) {
        try {
            final String secret = telegramProperties.getSecret();
            final String url = telegramProperties.getUrl();
            final String chatId = telegramProperties.getChatId();

            RestTemplate restTemplate = new RestTemplate();

            final HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("chat_id", chatId)
                    .queryParam("parse_mode", "HTML")
                    .queryParam("disable_web_page_preview", "true")
                    .queryParam("text", message);
            final HttpEntity<?> entity = new HttpEntity<>(headers);

            restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        } catch (Exception e) {
            log.error("텔레그램 메시지 전송시에 오류 발생함. [SKIP] [{}] ", message, e);
            throw e;
        }
    }

    private String getActiveFirstProfile() {
        if(environment.getActiveProfiles().length > 0) {
            return environment.getActiveProfiles()[0];
        }
        return "local";
    }

}