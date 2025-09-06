package com.tbm.admin.service.queue;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

public class RabbitMQApiTest {
    public static void main(String[] args) {
        String url = "http://43.200.165.243:15672/api/queues/%2F/repair-request-119.196.235.151";
        String username = "grow";
        String password = "@@grow!!";

        // 헤더 생성
        HttpHeaders headers = new HttpHeaders();

        // Basic Auth 설정
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        // Content-Type 설정
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 엔터티 생성
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // RestTemplate 인스턴스 생성
        RestTemplate restTemplate = new RestTemplate();

        try {
            // GET 요청 실행
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // 응답 처리
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Response:");
                System.out.println(response.getBody());
            } else {
                System.out.println("Error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
        }
    }
}
