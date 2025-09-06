package com.tbm.admin.service.crawler;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaverBlogCrawler {
    public static void main(String[] args) {
        // 요청할 URL
        String url = "https://blog.naver.com/PostView.naver?blogId=dhkdalrl80767&logNo=223770241703&redirect=Dlog&widgetTypeCall=true&topReferer=https%3A%2F%2Fsearch.naver.com%2Fsearch.naver%3Fwhere%3Dnexearch%26sm%3Dtop_hty%26fbm%3D0%26ie%3Dutf8%26query%3D%25EC%259D%25B8%25EC%25B2%259C%25ED%2598%2595%25EC%2582%25AC&trackingCode=nx&directAccess=false";

        // Referer를 변수로 두어 원하는 값으로 설정 가능
        String referer = "https://blog.naver.com/dhkdalrl80767/223770241703";

        // RestTemplate 생성
        RestTemplate restTemplate = new RestTemplate();

        // HttpHeaders 생성 및 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.set("accept-language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        // 필요에 따라 쿠키 부분은 변경/삭제할 수 있습니다.
        headers.set("cookie", "NAC=cderC4Akul7PB; BA_DEVICE=01e5a5eb-6845-46ca-af53-db41abd114e0; NNB=UFMFIU227J3WO; SRT30=1740291731; NACT=1; SRT5=1740313707; page_uid=iJVSgdqX5E0sssR6FxsssssssAC-202233; _naver_usersession_=INvDSDpp+WFgALHimSlucg==; BUC=vCGnH__uQN0Mpyut_bJ9S4qgCGQr1VpeXVUfn1yJHYs=; JSESSIONID=919AD9C38B3AC9D0A38B2ACC90F867C1.jvm1");
        headers.set("priority", "u=0, i");
        headers.set("referer", referer);
        headers.set("sec-ch-ua", "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"macOS\"");
        headers.set("sec-fetch-dest", "iframe");
        headers.set("sec-fetch-mode", "navigate");
        headers.set("sec-fetch-site", "same-origin");
        headers.set("upgrade-insecure-requests", "1");
        // 브라우저처럼 보이도록 User-Agent 설정
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");

        // HttpEntity 생성
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // GET 요청
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

        // 응답 받은 HTML
        String html = response.getBody();

        // <title> 태그 내용 추출 (정규식 예시)
        String title = null;
        if (html != null) {
            Pattern pattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                title = matcher.group(1).trim();
            }
        }

        // 결과 출력
        System.out.println("TITLE: " + title);
    }
}
