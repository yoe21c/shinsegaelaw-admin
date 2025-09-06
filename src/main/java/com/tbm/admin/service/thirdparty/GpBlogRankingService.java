package com.tbm.admin.service.thirdparty;

import com.tbm.admin.model.view.rank.GpRankData;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 블로그아이디로 검색해서 최근 포스팅 50개를 가져오고 특정 키워드에 대하여 해당 포스팅의 표기순위를 표시한다.
 * <p>
 * * 노출필드
 * 1. 작성일 : 검색된 포스팅의 포스팅날짜
 * 2. 제목 : 검색된 포스팅의 제목
 * 3. 상태
 * - 노출이란? 1위 ~ 100위안에 노출되는 경우
 * - Good : 쌍따옴표 없이 제목으로 검색했을 때 노출되었을 경우 [todo 진행중] => RestTemplate ,
 * - SoSo : 제목을 쌍따옴표로 감싸서 검색했을 때 노출되었을 경우
 * - Bad : 그외 의 경우
 * 4. 키워드
 * - 모든 포스팅에 포함되며 기본적으로 비워져 있음
 * - 키워드를 넣고 "검색"하여 해당 게시물이 몇번째 노출되는지를 값을 표기한다.
 * - 만약 100위 이내이면 "{순위값}" 을 표기한다.
 * - 만약 100위 밖이면 "순위권 밖" 이라고 표시한다.
 * <p>
 * 시의성이 있으므로 위 조회결과에 대해서는 따로 DB에 저장하지는 않는다.
 */
@Slf4j
@Service
public class GpBlogRankingService {

    /**
     * 포스팅 URL과 포스팅 제목을 받아서 해당 포스팅이 어떤 상태인지를 반환한다.
     *
     * @param postingUrl   포스팅 URL
     * @param postingTitle 포스팅 제목
     * @return
     */
    public GpRankData retrievePostingGrade(String postingUrl, String postingTitle) {

        // 첫번째에 결과가 있었다면 Good 임.
        final GpRankData firstRoundResult = searching(postingTitle);

        final int firstRanking = firstRoundResult.extractRanking(postingUrl);
        if (firstRanking > 0) {
            log.info("Good:{}", firstRanking);
            firstRoundResult.setGrade("Good");
            firstRoundResult.setRanking(firstRanking);
            return firstRoundResult;
        }

        // 없었으면 다시 조회. 타이틀을 쌍따옴표로 감싸서 조회 "searchTitle"
        final GpRankData secondRoundResult = searching("\"" + postingTitle + "\"");

        final int secondRanking = secondRoundResult.extractRanking(postingUrl);
        if (secondRanking > 0) {
            log.info("SoSo:{}", secondRanking);
            secondRoundResult.setGrade("SoSo");
            secondRoundResult.setRanking(secondRanking);
            return secondRoundResult;
        }

        firstRoundResult.setGrade("Bad");
        firstRoundResult.setRanking(-1);
        return firstRoundResult;
    }

    /**
     * 포스팅 URL과 포스팅 제목을 받아서 해당 포스팅이 어떤 상태인지를 반환한다.
     *
     * @param postingUrl 포스팅 URL
     * @param keyword    검색어
     * @return 몇등인지 반환
     */
    public int retrievePostingRanking(String postingUrl, String keyword) {
        final GpRankData firstRoundResult = searching(keyword);
        return firstRoundResult.extractRanking(postingUrl);
    }

    private GpRankData searching(String searchTitle) {

        // 첫페이지 조회
        GpRankData gpRankData = searchFirstPage(searchTitle);
        int iteration = Math.min(gpRankData.getTotalPage(), 8);

        // 스레드 풀 설정
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // 비동기 처리 리스트
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 두번째 페이지부터 max = 8까지 조회 => 15개씩 8이면 120개이므로 충분.
        for (int page = 2; page <= iteration; page++) {
            int currentPage = page;
            int start = 15 * (page - 2) + 1;
            log.info("title = {}, page = [{}], start = [{}]", searchTitle, page, start);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                searchPage(gpRankData, currentPage + "", start + "");
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        for (String url : gpRankData.getUrls()) {
            log.debug("url = [{}]", url);
        }

        // 스레드 풀 종료
        executor.shutdown();

        return gpRankData;
    }

    private GpRankData searchFirstPage(String searchTitle) {

        GpRankData gpRankData = new GpRankData();
        gpRankData.setKeyword(searchTitle);

        String url = "https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=0&ie=utf8&query=" + gpRankData.getKeyword();
        gpRankData.getReferers().add(url);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("referer", "https://www.naver.com/");

        addDefaultHeaders(headers);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET, entity, String.class);

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        StringBuilder cookieBuilder = new StringBuilder();
        if (cookies != null) {
            for (String cookie : cookies) {
                log.info("Set-Cookie: {}", cookie);
                final String valueString = cookie.split(";")[0];
                String name = valueString.split("=")[0];
                String value = valueString.split("=")[1];
                cookieBuilder.append(name).append("=").append(value).append("; ");
            }
        }

        final String cookie = cookieBuilder.toString().substring(0, cookieBuilder.length() - 2);
        gpRankData.getCookies().add(cookie);

        // Parse HTML with Jsoup
        Document doc = Jsoup.parse(response.getBody());
        Elements dscLinks = doc.select("a.dsc_link");
        for (Element anchor : dscLinks) {
            final String href = anchor.attr("href");
            log.debug("href = [{}]", href);
            gpRankData.getUrls().add(href);
        }

        // 페이지 번호를 포함하는 링크 요소 선택
        Elements pageLinks = doc.select(".sc_page_inner a.btn");

        // 페이지 번호들 중 가장 큰 값 찾기
        int totalPages = 0;
        for (Element pageLink : pageLinks) {
            String pageNumberText = pageLink.text();
            try {
                int pageNumber = Integer.parseInt(pageNumberText);
                if (pageNumber > totalPages) {
                    totalPages = pageNumber;
                }
            } catch (NumberFormatException e) {
                // 무시 - 페이지 번호가 아닌 경우
            }
        }

        log.info("title: {}, 총 페이지 수: {}", searchTitle, totalPages);
        gpRankData.setTotalPage(totalPages);

        return gpRankData;
    }

    private String searchPage(GpRankData gpRankData, String page, String start) {

        String url = String.format("https://search.naver.com/search.naver?nso=&page=%s&query=%s&sm=tab_pge&start=%s&where=web",
                page, gpRankData.getKeyword(), start);
        gpRankData.getReferers().add(url);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("referer", gpRankData.lastReferer());
        headers.add("cookie", gpRankData.lastCookie());

        addDefaultHeaders(headers);
        int attempts = 0;
        int maxAttemptsCnt = 10;
        while (attempts < maxAttemptsCnt) {
            try {

                String userAgent = this.getRandomUserAgents();
                headers.add("user-agent", userAgent);

                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET, entity, String.class);

                List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                StringBuilder cookieBuilder = new StringBuilder();
                if (cookies != null) {
                    for (String cookie : cookies) {
                        log.info("Set-Cookie: {}", cookie);
                        final String valueString = cookie.split(";")[0];
                        String name = valueString.split("=")[0];
                        String value = valueString.split("=")[1];
                        cookieBuilder.append(name).append("=").append(value).append("; ");
                    }
                }

                final String cookie = cookieBuilder.toString().substring(0, cookieBuilder.length() - 2);

                // Parse HTML with Jsoup
                Document doc = Jsoup.parse(response.getBody());
                Elements dscLinks = doc.select("a.link_tit");
                for (Element anchor : dscLinks) {
                    final String href = anchor.attr("href");
                    log.debug("href = [{}]", href);
                    gpRankData.getUrls().add(href);
                }

                return cookie;
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    attempts++;
                    System.out.println("403 에러 발생. User-Agent를 변경하고 재시도 중... 시도 횟수: " + attempts);
                    if (attempts >= maxAttemptsCnt) {
                        throw new RuntimeException("403 에러로 인해 최대 재시도 횟수에 도달했습니다.", e);
                    }
                } else {
                    throw e;
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return null;
    }

    private void addDefaultHeaders(HttpHeaders headers) {
        headers.add("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.add("accept-language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.add("cache-control", "no-cache");
        headers.add("pragma", "no-cache");
        headers.add("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        headers.add("sec-ch-ua-arch", "\"arm\"");
        headers.add("sec-ch-ua-bitness", "\"64\"");
        headers.add("sec-ch-ua-full-version-list", "\"Google Chrome\";v=\"123.0.6312.123\", \"Not:A-Brand\";v=\"8.0.0.0\", \"Chromium\";v=\"123.0.6312.123\"");
        headers.add("sec-ch-ua-mobile", "?0");
        headers.add("sec-ch-ua-model", "\"\"");
        headers.add("sec-ch-ua-platform", "macOS");
        headers.add("sec-ch-ua-platform-version", "14.4.1");
        headers.add("sec-ch-ua-wow64", "?0");
        headers.add("sec-fetch-dest", "document");
        headers.add("sec-fetch-mode", "navigate");
        headers.add("sec-fetch-site", "same-site");
        headers.add("sec-fetch-user", "?1");
        headers.add("upgrade-insecure-requests", "1");
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
    }

    private String getRandomUserAgents() {
        List<String> userAgents = Arrays.asList(
                // Chrome (Windows)
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36",

                // Chrome (Mac)
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36",

                // Firefox (Windows)
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:87.0) Gecko/20100101 Firefox/87.0",

                // Firefox (Mac)
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:88.0) Gecko/20100101 Firefox/88.0",

                // Safari (Mac)
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.2 Safari/605.1.15",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1.2 Safari/605.1.15",

                // Edge (Windows)
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.64",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36 Edg/90.0.818.56",

                // iPhone Safari
                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15A372 Safari/604.1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 13_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0 Mobile/15A372 Safari/604.1",

                // Android Chrome
                "Mozilla/5.0 (Linux; Android 11; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.105 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 10; SM-G950F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.93 Mobile Safari/537.36",

                // Android Firefox
                "Mozilla/5.0 (Android 11; Mobile; rv:89.0) Gecko/89.0 Firefox/89.0",
                "Mozilla/5.0 (Android 10; Mobile; rv:88.0) Gecko/88.0 Firefox/88.0",

                // iPad Safari
                "Mozilla/5.0 (iPad; CPU OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15A5341f Safari/604.1",
                "Mozilla/5.0 (iPad; CPU OS 13_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0 Mobile/15A5341f Safari/604.1"
        );
        Random random = new Random();
        return userAgents.get(random.nextInt(userAgents.size()));
    }

}
