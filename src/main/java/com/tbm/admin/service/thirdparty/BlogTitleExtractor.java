package com.tbm.admin.service.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BlogTitleExtractor {

    public String extractTitle(String firstUrl) {

        // RestTemplate 생성
        RestTemplate restTemplate = new RestTemplate();

        // 첫 번째 요청용 헤더 생성
        HttpHeaders firstHeaders = new HttpHeaders();
        firstHeaders.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                       "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
        // 필요하다면 accept, accept-language 등 다른 헤더도 추가 가능
        // firstHeaders.set("accept", "...");

        // 첫 번째 GET 요청
        HttpEntity<String> firstRequestEntity = new HttpEntity<>(firstHeaders);
        ResponseEntity<String> firstResponse = restTemplate.exchange(
            firstUrl,
            HttpMethod.GET,
            firstRequestEntity,
            String.class
        );

        // 응답 HTML
        String firstHtml = firstResponse.getBody();

        // 1) HTML에서 <iframe> 태그의 src 추출 (정규식 예시)
        String iframeSrc = null;
        if (firstHtml != null) {
            Pattern iframePattern = Pattern.compile("<iframe[^>]+src=\"([^\"]+)\"");
            Matcher matcher = iframePattern.matcher(firstHtml);
            if (matcher.find()) {
                iframeSrc = matcher.group(1);
            }
        }

        log.info("Extracted Iframe src: {}", iframeSrc);

        // 2) 0.5초 정도 sleep
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 3) 두 번째 요청
        // referer 는 첫 번째 URL 값으로 지정
        String referer = firstUrl;

        // iframeSrc 앞에 공통 URL 붙이기
        String finalUrl = "https://blog.naver.com" + iframeSrc;

        // 두 번째 요청용 헤더 만들기
        HttpHeaders secondHeaders = new HttpHeaders();
        secondHeaders.set("accept", "text/html,application/xhtml+xml,application/xml;q=0.9," +
                                    "image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        secondHeaders.set("accept-language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        // 필요 시 쿠키도 추가 가능(아래는 예시)
        // secondHeaders.set("cookie", "NAC=cderC4Akul7PB; ...");
        secondHeaders.set("referer", referer);
        secondHeaders.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
        secondHeaders.set("upgrade-insecure-requests", "1");

        // 두 번째 GET 요청
        HttpEntity<String> secondRequestEntity = new HttpEntity<>(secondHeaders);
        ResponseEntity<String> secondResponse = restTemplate.exchange(
            finalUrl,
            HttpMethod.GET,
            secondRequestEntity,
            String.class
        );

        // 두 번째 응답 HTML
        String secondHtml = secondResponse.getBody();

        // <title> 태그 내용 추출 (정규식 예시)
        String title = null;
        if (secondHtml != null) {
            Pattern titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = titlePattern.matcher(secondHtml);
            if (matcher.find()) {
                title = matcher.group(1).trim();
            }
        }

        String finalTitle = title.split(" : ")[0];
        // 최종 결과 출력
        log.info("Title: {}, Final Title: {}", title, finalTitle);

        return finalTitle;
    }
}
