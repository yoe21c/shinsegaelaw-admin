package com.tbm.admin.service.thirdparty;

import com.tbm.admin.model.view.rank.GpRankData;
import com.tbm.admin.model.view.rank.NaverBlogResponse;
import com.tbm.admin.model.view.rank.NaverPost;
import com.tbm.admin.model.view.rank.NaverSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.tbm.admin.utils.Utils.toObject;

@Slf4j
class GpBlogRankingServiceTest {

    @Test
    public void testNaverBlogTester() throws Exception {

        String urlFormat = "https://blog.naver.com/PostTitleListAsync.naver?blogId=%s&viewdate=&currentPage=%s&categoryNo=%s&parentCategoryNo=&countPerPage=%s";

        String blogId = "yoonyiqq";
        String categoryNo = "18";
        String countPerPage = "30";

        List<NaverPost> list = new ArrayList<>();

        // Define the URL 2페이지까지만 돌린다.
        String referer = null;
        String cookie = null;
        for (int pageNumber = 1; pageNumber <= 2; pageNumber++) {

            String url = String.format(urlFormat, blogId, pageNumber, categoryNo, countPerPage);

            cookie = searchBlogPosting(pageNumber, url, blogId, referer, cookie, list);
            log.info("page {} is done", pageNumber);

            if( cookie == null) {
                break;
            }
            referer = url;
            log.info("next page ! ");
        }

        System.out.println("==============================================");
        for (NaverPost naverPost : list) {
            System.out.println("naverPost = [" + naverPost + "]");
        }

    }

    private String searchBlogPosting(int pageNumber, String url, String blogId, String referer, String cookie, List<NaverPost> list) {

        RestTemplate restTemplate = new RestTemplate();

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.set("accept-language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("cache-control", "no-cache");
        headers.set("pragma", "no-cache");
        headers.set("priority", "u=0, i");
        headers.set("sec-ch-ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"macOS\"");
        headers.set("sec-fetch-dest", "document");
        headers.set("sec-fetch-mode", "navigate");
        headers.set("sec-fetch-site", "none");
        headers.set("sec-fetch-user", "?1");
        headers.set("upgrade-insecure-requests", "1");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");

        if(referer != null) {
            headers.add("referer", referer);
        }

        if(cookie != null) {
            headers.add("cookie", cookie);
        }

        // Create the request entity
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Execute the GET request
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        // Print the response
        String responseBody = response.getBody();
        System.out.println("responseBody = [" + responseBody + "]");

        assert responseBody != null;
        responseBody = responseBody.replaceAll("\\\\", "");

        final NaverBlogResponse naverBlogResponse = toObject(responseBody, NaverBlogResponse.class);
        System.out.println("naverBlogResponse = [" + naverBlogResponse + "]");

        String blogUrlFormat = "https://blog.naver.com/%s/%s";

        for (NaverBlogResponse.Post post : naverBlogResponse.getPostList()) {
            log.info("logNo: {}, categoryNo: {}, title: {}, addDate: {}",
                post.getLogNo(), post.getCategoryNo(), post.getTitle(), post.getAddDate());

            String blogUrl = String.format(blogUrlFormat, blogId, post.getLogNo());

            list.add(new NaverPost(blogId, blogUrl, post.getTitle(), post.getAddDate()));
        }

        final int resultCountPerPage = Integer.parseInt(naverBlogResponse.getCountPerPage());
        final int resultTotalCount = Integer.parseInt(naverBlogResponse.getTotalCount());

        log.info("(resultCountPerPage * pageNumber) : {}, resultTotalCount : {}",
            (resultCountPerPage * pageNumber), resultTotalCount);

        if(resultTotalCount > (resultCountPerPage * pageNumber)) {
            return extractCookie(response);
        }

        return null;
    }

    @Test
    public void testInternalFeedServer() {
        String url = "http://52.78.207.105:9096/api/v1/search-post?blogUrl=https://blog.naver.com/seatomint&limit=30";

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", "tbm-feed");

        // 엔티티 생성
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // RestTemplate 객체 생성
        RestTemplate restTemplate = new RestTemplate();

        // GET 요청 보내기
        ResponseEntity<NaverSearchResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, NaverSearchResponse.class);

        // 응답 데이터 출력
        if(response.getBody() != null) {
            response.getBody().getList().forEach(post -> {
                System.out.println("URL: " + post.getUrl());
                System.out.println("Title: " + post.getTitle());
                System.out.println("Posted At: " + post.getPostedAt());
            });
        } else {
            System.out.println("No data found");
        }
    }

    @Test
    public void testRetrievePostingGrade() {
        GpBlogRankingService gpBlogRankingService = new GpBlogRankingService();
        // https://blog.naver.com/seatomint
        final GpRankData gpRankData = gpBlogRankingService.retrievePostingGrade("https://blog.naver.com/seatomint/223516275153", "이혼 재산분할 10년 기간 변론 논점은");
        System.out.println("gpRankData = [" + gpRankData + "]");
    }

    @Test
    public void testRetrievePostingRanking() {
        GpBlogRankingService gpBlogRankingService = new GpBlogRankingService();
        final int ranking = gpBlogRankingService.retrievePostingRanking("https://blog.naver.com/seatomint/223516275153", "이혼 재산분할 10년");
        System.out.println("ranking = [" + ranking + "]");
    }

    @Test
    public void testEncoding() {
        // %EC%95%84%EB%8F%99%ED%95%99%EB%8C%80%EB%B3%80%ED%98%B8%EC%82%AC+%EB%B2%95%EC%A0%81+%EC%A1%B0%EC%9C%A8%EC%9D%80
        // %EC%95%84%EB%8F%99%ED%95%99%EB%8C%80%EB%B3%80%ED%98%B8%EC%82%AC+%EB%B2%95%EC%A0%81+%EC%A1%B0%EC%96%B8%EC%9D%80
        String keyword = "아동학대변호사 법적 조언은";
        try {
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
            System.out.println("Encoded Keyword: " + encodedKeyword);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        final String decode = URLDecoder.decode("%EC%95%84%EB%8F%99%ED%95%99%EB%8C%80%EB%B3%80%ED%98%B8%EC%82%AC+%EB%B2%95%EC%A0%81+%EC%A1%B0%EC%96%B8%EC%9D%80", StandardCharsets.UTF_8);
        System.out.println("decode = [" + decode + "]");
    }

    @Test
    public void test2() throws UnsupportedEncodingException {

//        String searchKeyword = "아동학대변호사 법적 조언은";
        String searchKeyword = "\"부산이혼소송변호사 자문 입증을\"";
        GpRankData gpRankData = searchFirstPage(searchKeyword);

        // 2~8 페이지까지 조회한다. 한페이지에 15개씩 아이템이 있다.
        // page = 2, start = 1
        // page = 3, start = 16
        // page = 4, start = 31

        int iteration = Math.min(gpRankData.getTotalPage(), 8);

        for (int page = 2; page <= iteration; page++) {
            int start = 15 * (page - 2) + 1;
            System.out.println("------------> page = [" + page + "], start = [" + start + "]");
            searchPage(gpRankData, page + "", start + "");
        }

        System.out.println("============================================================");
        System.out.println("============================================================");

        for (String url : gpRankData.getUrls()) {
            System.out.println("url = [" + url + "]");
        }

        System.out.println("done !!");
    }



    private static GpRankData searchFirstPage(String searchKeyword) {

        GpRankData gpRankData = new GpRankData();
        gpRankData.setKeyword(searchKeyword);

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

        final String cookie = extractCookie(response);
        gpRankData.getCookies().add(cookie);

        // Parse HTML with Jsoup
        Document doc = Jsoup.parse(response.getBody());
        Elements dscLinks = doc.select("a.dsc_link");
        for (Element anchor : dscLinks) {
            final String href = anchor.attr("href");
            System.out.println("href = [" + href + "]");
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

        System.out.println("총 페이지 수: " + totalPages);
        gpRankData.setTotalPage(totalPages);

        return gpRankData;
    }

    // todo 여기 검증할 차례.
    private String searchPage(GpRankData gpRankData, String page, String start) {

        String url = String.format("https://search.naver.com/search.naver?nso=&page=%s&query=%s&sm=tab_pge&start=%s&where=web",
            page, gpRankData.getKeyword(), start);
        gpRankData.getReferers().add(url);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("referer", gpRankData.lastReferer());
        headers.add("cookie", gpRankData.lastCookie());

        addDefaultHeaders(headers);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET, entity, String.class);

        final String cookie = extractCookie(response);

        // Parse HTML with Jsoup
        Document doc = Jsoup.parse(response.getBody());
        Elements dscLinks = doc.select("a.link_tit");
        for (Element anchor : dscLinks) {
            final String href = anchor.attr("href");
            System.out.println("href = [" + href + "]");
            gpRankData.getUrls().add(href);
        }

        return cookie;
    }

    private static String extractCookie(ResponseEntity<String> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        StringBuilder cookieBuilder = new StringBuilder();
        if (cookies != null) {
            for (String cookie : cookies) {
                System.out.println("Set-Cookie: " + cookie);
                final String valueString = cookie.split(";")[0];
                String name = valueString.split("=")[0];
                String value = valueString.split("=")[1];
                cookieBuilder.append(name).append("=").append(value).append("; ");
            }
        }

        return cookieBuilder.substring(0, cookieBuilder.length() - 2);
    }

    private static void addDefaultHeaders(HttpHeaders headers) {
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
}