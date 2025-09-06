package com.tbm.admin.service.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tbm.admin.utils.Utils.toJson;

@Slf4j
@Service
public class BlogMatrixService {

    /**
     * 블로그 매트릭스에서 블로그를 조회한다.
     * @param memberSeq
     * @return
     */
    public CrawlingData searchUserInfo(Long memberSeq) {

        log.info("searchUserInfo memberSeq: {}", memberSeq);

        // 1. Get the CSRF token and the Set-Cookie header
        CrawlingData crawlingData = getLoginPage();
        crawlingData = login(crawlingData);
        crawlingData = loadingLandingPage(crawlingData);

        try {
            Thread.sleep(2000); // 2초 대기
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        crawlingData = goBlogMatrixMenuPage(crawlingData);

        log.info("Final Result: {}", toJson(crawlingData));

        return crawlingData;

    }

    /**
     * 블로그 매트릭스에서 블로그를 조회한다.
     * @param memberSeq
     * @param blogId 블로그 아이디 ex: "imvgw28841"
     * @return
     */
    public CrawlingData searchBlogMatrix(Long memberSeq, String blogId) {

        log.info("searchBlogMatrix memberSeq: {}, blogId: {}", memberSeq, blogId);

        // 1. Get the CSRF token and the Set-Cookie header
        CrawlingData crawlingData = getLoginPage();
        crawlingData = login(crawlingData);
        crawlingData = loadingLandingPage(crawlingData);

        try {
            Thread.sleep(2000); // 2초 대기
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        crawlingData = goBlogMatrixMenuPage(crawlingData);

        try {
            Thread.sleep(1000); // 2초 대기
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 1 page 조회
        log.info("1 page 조회");
        crawlingData = goSearchingBlog(crawlingData, blogId, 1);
        crawlingData = searching(crawlingData); // 실제로 조회한다.
        crawlingData.getTotalPostDetails().addAll(crawlingData.getPostDetails());
        crawlingData.setPostDetails(new ArrayList<>()); // 초기화 (다음 페이지 조회를 위해)

        // 2 page 부터 조회
        for (int i = 2; i <= crawlingData.getTotalPages(); i++) {
            log.info("{} page 조회", i);
            crawlingData = goSearchingBlog(crawlingData, blogId, i);
            crawlingData = searching(crawlingData); // 실제로 조회한다.
            crawlingData.getTotalPostDetails().addAll(crawlingData.getPostDetails());
            crawlingData.setPostDetails(new ArrayList<>()); // 초기화 (다음 페이지 조회를 위해)
        }

        log.info("Final Result: {}", toJson(crawlingData));

        return crawlingData;

    }

    private static CrawlingData getLoginPage() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://blogmatrix.co.kr/login.php";

        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.add("accept-language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.add("cache-control", "no-cache");
        headers.add("pragma", "no-cache");
        headers.add("referer", "https://blogmatrix.co.kr/");
        headers.add("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        headers.add("sec-ch-ua-mobile", "?0");
        headers.add("sec-ch-ua-platform", "\"macOS\"");
        headers.add("sec-fetch-dest", "document");
        headers.add("sec-fetch-mode", "navigate");
        headers.add("sec-fetch-site", "same-origin");
        headers.add("sec-fetch-user", "?1");
        headers.add("upgrade-insecure-requests", "1");
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        // Get the Set-Cookie header
        String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assert setCookieHeader != null;
        final String myCookie = setCookieHeader.split(";")[0];
        log.info("Set-Cookie: {}, MyCookie: {}", setCookieHeader, myCookie);

        // Parse the HTML to extract the CSRF token
        Document doc = Jsoup.parse(response.getBody());
        Element csrfElement = doc.selectFirst("input[name=csrf_token]");
        String csrfToken = csrfElement.attr("value");
        log.info("CSRF Token: {}", csrfToken);

        CrawlingData data = new CrawlingData();
        data.setPhpSessionIdCookie(myCookie);
        data.setCsrfToken(csrfToken);

        return data;
    }

    private static CrawlingData login(CrawlingData crawlingData) {
        // Proceed to login
        String loginUrl = "https://blogmatrix.co.kr/login_ok.php";

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.add("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        loginHeaders.add("accept-language", "ko-KR,ko;q=0.9");
        loginHeaders.add("cache-control", "no-cache");
        loginHeaders.add("content-type", "application/x-www-form-urlencoded");
        loginHeaders.add("cookie", crawlingData.getPhpSessionIdCookie());
        loginHeaders.add("origin", "https://blogmatrix.co.kr");
        loginHeaders.add("pragma", "no-cache");
        loginHeaders.add("referer", "https://blogmatrix.co.kr/login.php");
        loginHeaders.add("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        loginHeaders.add("sec-ch-ua-mobile", "?0");
        loginHeaders.add("sec-ch-ua-platform", "\"macOS\"");
        loginHeaders.add("sec-fetch-dest", "document");
        loginHeaders.add("sec-fetch-mode", "navigate");
        loginHeaders.add("sec-fetch-site", "same-origin");
        loginHeaders.add("sec-fetch-user", "?1");
        loginHeaders.add("upgrade-insecure-requests", "1");
        loginHeaders.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");

        MultiValueMap<String, String> loginParams = new LinkedMultiValueMap<>();
        loginParams.add("user_id", "adadyy");
        loginParams.add("user_pw", "adad2120");
        loginParams.add("csrf_token", crawlingData.getCsrfToken());

        HttpEntity<MultiValueMap<String, String>> loginEntity = new HttpEntity<>(loginParams, loginHeaders);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> loginResponse = restTemplate.exchange(loginUrl, HttpMethod.POST, loginEntity, String.class);

        // Get the crxpblogmatrix cookie value
        HttpHeaders loginResponseHeaders = loginResponse.getHeaders();
        List<String> setCookies = loginResponseHeaders.get(HttpHeaders.SET_COOKIE);
        String crxpblogmatrixCookie = setCookies.stream()
            .filter(cookie -> cookie.startsWith("crxpblogmatrix"))
            .findFirst()
            .orElse("");

        final String myCookie = crxpblogmatrixCookie.split(";")[0];
        log.info("crxpblogmatrix Cookie: {}, myCookie: {}", crxpblogmatrixCookie, myCookie);

        crawlingData.setCrxpblogmatrixCookie(myCookie);
        return crawlingData;
    }

    private static CrawlingData loadingLandingPage(CrawlingData crawlingData) {
        // Step 3: Request homepage and extract auth0 value
        String homepageUrl = "https://blogmatrix.co.kr/";
        String cookie = crawlingData.getPhpSessionIdCookie() + "; " + crawlingData.getCrxpblogmatrixCookie();
        HttpHeaders homepageHeaders = new HttpHeaders();
        homepageHeaders.add("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        homepageHeaders.add("accept-language", "ko-KR,ko;q=0.9");
        homepageHeaders.add("cache-control", "no-cache");
        homepageHeaders.add("cookie", cookie);
        homepageHeaders.add("pragma", "no-cache");
        homepageHeaders.add("referer", "https://blogmatrix.co.kr/login_ok.php");
        homepageHeaders.add("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        homepageHeaders.add("sec-ch-ua-mobile", "?0");
        homepageHeaders.add("sec-ch-ua-platform", "\"macOS\"");
        homepageHeaders.add("sec-fetch-dest", "document");
        homepageHeaders.add("sec-fetch-mode", "navigate");
        homepageHeaders.add("sec-fetch-site", "same-origin");
        homepageHeaders.add("upgrade-insecure-requests", "1");
        homepageHeaders.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");

        HttpEntity<String> homepageEntity = new HttpEntity<>(homepageHeaders);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> homepageResponse = restTemplate.exchange(homepageUrl, HttpMethod.GET, homepageEntity, String.class);

        // Parse the homepage HTML to extract the auth0 value
        Document homepageDoc = Jsoup.parse(homepageResponse.getBody());
        Element auth0Element = homepageDoc.selectFirst("script:containsData(var auth0 =)");
        String auth0ScriptContent = auth0Element.data();
        String auth0Value = auth0ScriptContent.split("'")[1];
        log.info("Auth0 Value: {}", auth0Value);

        // Use the extracted auth0 value as needed
        String authenticationCode = auth0Value;
        log.info("Authentication Code: {}", authenticationCode);

        crawlingData.setAuthenticationCode(authenticationCode);
        return crawlingData;
    }

    private static CrawlingData goBlogMatrixMenuPage(CrawlingData crawlingData) {
        // Step 3: Request homepage and extract auth0 value
        String homepageUrl = "https://blogmatrix.co.kr/?menu=whereispost";
        String cookie = crawlingData.getPhpSessionIdCookie() + "; " + crawlingData.getCrxpblogmatrixCookie();
        HttpHeaders homepageHeaders = new HttpHeaders();
        homepageHeaders.add("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        homepageHeaders.add("accept-language", "ko-KR,ko;q=0.9");
        homepageHeaders.add("cache-control", "no-cache");
        homepageHeaders.add("cookie", cookie);
        homepageHeaders.add("pragma", "no-cache");
        homepageHeaders.add("referer", "https://blogmatrix.co.kr/");
        homepageHeaders.add("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        homepageHeaders.add("sec-ch-ua-mobile", "?0");
        homepageHeaders.add("sec-ch-ua-platform", "\"macOS\"");
        homepageHeaders.add("sec-fetch-dest", "document");
        homepageHeaders.add("sec-fetch-mode", "navigate");
        homepageHeaders.add("sec-fetch-site", "same-origin");
        homepageHeaders.add("upgrade-insecure-requests", "1");
        homepageHeaders.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");

        HttpEntity<String> homepageEntity = new HttpEntity<>(homepageHeaders);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> homepageResponse = restTemplate.exchange(homepageUrl, HttpMethod.GET, homepageEntity, String.class);

        // Parse the homepage HTML to extract the auth0 value
        Document document = Jsoup.parse(homepageResponse.getBody());
        Element auth0Element = document.selectFirst("script:containsData(var auth0 =)");
        String auth0ScriptContent = auth0Element.data();
        String auth0Value = auth0ScriptContent.split("'")[1];
        log.info("Auth0 Value: {}", auth0Value);

        // Use the extracted auth0 value as needed
        String authenticationCode = auth0Value;
        log.info("Authentication Code: {}", authenticationCode);

        // span 태그 중 class가 bsccnt인 요소를 찾습니다.
        Elements bsccntElements = document.select("span.bsccnt");
        for (Element bsccntElement : bsccntElements) {
            String bsccnt = bsccntElement.text();
            log.info("usedCount ! : {}", bsccnt);
            crawlingData.setUsedCount(Integer.parseInt(bsccnt));
        }

        final Element userInfoElement = document.selectFirst(".user-info");
        assert userInfoElement != null;

        log.info("UserInfo: {}", userInfoElement.text());

        crawlingData.setUserInfo(userInfoElement.text());

        crawlingData.setAuthenticationCode(authenticationCode);
        return crawlingData;
    }

    private static CrawlingData goSearchingBlog(CrawlingData crawlingData, String blogId, Integer page) {
        // Step 4: Perform the search and extract post details
        String searchUrl = "https://blogmatrix.co.kr/?menu=whereispost";
        String cookie = crawlingData.getPhpSessionIdCookie() + "; " + crawlingData.getCrxpblogmatrixCookie();

        HttpHeaders searchHeaders = new HttpHeaders();
        searchHeaders.add("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        searchHeaders.add("accept-language", "ko-KR,ko;q=0.9");
        searchHeaders.add("cache-control", "no-cache");
        searchHeaders.add("content-type", "application/x-www-form-urlencoded");
        searchHeaders.add("cookie", cookie);
        searchHeaders.add("origin", "https://blogmatrix.co.kr");
        searchHeaders.add("pragma", "no-cache");
        searchHeaders.add("referer", "https://blogmatrix.co.kr/?menu=whereispost");
        searchHeaders.add("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        searchHeaders.add("sec-ch-ua-mobile", "?0");
        searchHeaders.add("sec-ch-ua-platform", "\"macOS\"");
        searchHeaders.add("sec-fetch-dest", "document");
        searchHeaders.add("sec-fetch-mode", "navigate");
        searchHeaders.add("sec-fetch-site", "same-origin");
        searchHeaders.add("sec-fetch-user", "?1");
        searchHeaders.add("upgrade-insecure-requests", "1");
        searchHeaders.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");

        MultiValueMap<String, String> searchParams = new LinkedMultiValueMap<>();
        searchParams.add("blogurl", blogId);

        if(page >= 2) {
            searchParams.add("pageno", page.toString());
        }

        HttpEntity<MultiValueMap<String, String>> searchEntity = new HttpEntity<>(searchParams, searchHeaders);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> searchResponse = restTemplate.exchange(searchUrl, HttpMethod.POST, searchEntity, String.class);

        // Parse the search result HTML to extract post details
        Document document = Jsoup.parse(searchResponse.getBody());
        Elements rows = document.select("table tbody tr");

        // Update the code for extracting post details
        List<PostDetail> postDetails = new ArrayList<>();
        for (Element row : rows) {
            String date = row.selectFirst("td.d-none.d-lg-table-cell").text();
            Element titleDivElement = row.select("td").select("div").first();
            String title = "";
            if(titleDivElement != null) {
                title = titleDivElement.text();
            }
            String url = row.select("a").attr("href");
            url = url.split("\\?")[0]; // Remove query string
            String dataSize = row.selectFirst("td.text-center div.blogurl").attr("data-size");

            postDetails.add(new PostDetail(date, title, url, dataSize));
        }

// Output the extracted post details
        for (PostDetail detail : postDetails) {
            log.info("Date: {}", detail.getDate());
            log.info("Title: {}", detail.getTitle());
            log.info("URL: {}", detail.getUrl());
            log.info("Data Size: {}", detail.getDataSize());
            log.info("\n");
        }

        crawlingData.setPostDetails(postDetails);

        // Additional code for extracting the total number of pages
        Element paginationElement = document.selectFirst("ul.pagination");
        int totalPages = 1; // Default to 1 if pagination element is not found

        if (paginationElement != null) {
            Elements pageItems = paginationElement.select("li.page-item");
            for (Element pageItem : pageItems) {
                Element pageLink = pageItem.selectFirst("a.page-link");
                if (pageLink != null) {
                    String onClickValue = pageLink.attr("OnClick");
                    if (onClickValue.startsWith("page(")) {
                        try {
                            int startIndex = onClickValue.indexOf("'") + 1;
                            int endIndex = onClickValue.lastIndexOf("'");
                            String pageNumberStr = onClickValue.substring(startIndex, endIndex);
                            int pageNumber = Integer.parseInt(pageNumberStr);
                            if (pageNumber > totalPages) {
                                totalPages = pageNumber;
                            }
                        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                            // Ignore any parsing errors
                        }
                    }
                }
            }
        }

        // span 태그 중 class가 bsccnt인 요소를 찾습니다.
        Elements bsccntElements = document.select("span.bsccnt");
        for (Element bsccntElement : bsccntElements) {
            String bsccnt = bsccntElement.text();
            log.info("bsccnt ------->>> {}", bsccnt);
            crawlingData.setUsedCount(Integer.parseInt(bsccnt));
        }

        log.info("Total Pages: {}", totalPages);
        crawlingData.setTotalPages(totalPages);
        return crawlingData;
    }

    private static CrawlingData searching(CrawlingData crawlingData) {
        // Step 4: Perform the search and extract post details
        String cookie = crawlingData.getPhpSessionIdCookie() + "; " + crawlingData.getCrxpblogmatrixCookie();

        log.info("Searching blog: {}, cookie: {}", crawlingData.getBlogId(), cookie);

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "text/plain, */*; q=0.01");
        headers.set("accept-language", "ko-KR,ko;q=0.9");
        headers.set("cache-control", "no-cache");
        headers.set("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("cookie", cookie);
        headers.set("origin", "https://blogmatrix.co.kr");
        headers.set("pragma", "no-cache");
        headers.set("referer", "https://blogmatrix.co.kr/?menu=whereispost");
        headers.set("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"macOS\"");
        headers.set("sec-fetch-dest", "empty");
        headers.set("sec-fetch-mode", "cors");
        headers.set("sec-fetch-site", "same-origin");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
        headers.set("x-requested-with", "XMLHttpRequest");

        // URL 설정
        String url = "https://blogmatrix.co.kr/checkpost.php";


        for (PostDetail postDetail : crawlingData.getPostDetails()) {

            String body = "size=" + postDetail.getDataSize() + "&auth0=" + crawlingData.getAuthenticationCode();
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            // 요청 보내기
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = null;
            try {
                response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            }catch (Exception e) {
                log.error("Continue ! checkpost.php Error: {}", e.getMessage());
                postDetail.setResult("Error:" + e.getMessage());
                continue;
            }

            // 응답 HTML 파싱
            Document document = Jsoup.parse(Objects.requireNonNull(response.getBody()));
            Element spanElement = document.selectFirst("span");

            // 결과 추출
            if (spanElement != null) {
                String result = spanElement.text();
                log.info("Result: {}", result);
                postDetail.setResult(result);
            } else {
                log.info("Element not found");
            }

        }

        return crawlingData;
    }
}
