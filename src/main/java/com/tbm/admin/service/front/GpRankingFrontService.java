package com.tbm.admin.service.front;

import com.tbm.admin.config.AesConfig;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.rank.GpRankData;
import com.tbm.admin.model.view.rank.NaverBlogResponse;
import com.tbm.admin.model.view.rank.NaverPost;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.thirdparty.GpBlogRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import static com.tbm.admin.utils.Utils.toObject;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpRankingFrontService {

    private final GpBlogRankingService gpBlogRankingService;

    private final AesConfig aesConfig;

    String urlFormat = "https://blog.naver.com/PostTitleListAsync.naver?blogId=%s&viewdate=&currentPage=%s&parentCategoryNo=&countPerPage=%s";

    public RestResult mappingAccountInfo() {

        String postingUrl = "https://blog.naver.com/seatomint/223516275153";
        String title = "이혼 재산분할 10년 기간 변론 논점은";

        final GpRankData gpRankData = gpBlogRankingService.retrievePostingGrade(postingUrl, title);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("data", gpRankData);
        return new RestResult(data);
    }

    public DataTableView getGpBlogRankings(MultiValueMap<String, String> param, Long memberSeq) {

        String blogId = StringUtils.isNotBlank(param.get("blogId").get(0)) ? param.get("blogId").get(0) : "";
        String categoryNo = StringUtils.isNotBlank(param.get("categoryNo").get(0)) ? param.get("categoryNo").get(0) : "";
        String countPerPage = StringUtils.isNotBlank(param.get("countPerPage").get(0)) ? param.get("countPerPage").get(0) : "";
        String currentPage = StringUtils.isNotBlank(param.get("currentPage").get(0)) ? param.get("currentPage").get(0) : "";
        int draw = StringUtils.isNotBlank(param.get("draw").get(0)) ? Integer.parseInt(param.get("draw").get(0)) : 0;

        if(StringUtils.isBlank(blogId)) {

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("list", Collections.EMPTY_LIST);
            return new DataTableView(0, 0, 0, data);
        }

        List<NaverPost> list = new ArrayList<>();

        // Define the URL 2페이지까지만 돌린다.
        String url = String.format(urlFormat, blogId, currentPage, countPerPage);
        if(!categoryNo.isEmpty()) {
            url += "&categoryNo=".concat(categoryNo);
        }

        String totalPageCount = searchBlogPosting(Integer.parseInt(currentPage), url, blogId, list);

        for (NaverPost naverPost : list) {
            final GpRankData gpRankData = gpBlogRankingService.retrievePostingGrade(naverPost.getUrl(), naverPost.getTitle());
            naverPost.setRanking(gpRankData.getRanking());
            naverPost.setStatus(gpRankData.getGrade());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        return new DataTableView(draw, Integer.parseInt(totalPageCount), Integer.parseInt(totalPageCount), data);
    }

    private String searchBlogPosting(int pageNumber, String url, String blogId, List<NaverPost> list) {

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

        // Create the request entity
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Execute the GET request
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        // Print the response
        String responseBody = response.getBody();

        assert responseBody != null;
        responseBody = responseBody.replaceAll("\\\\", "");

        final NaverBlogResponse naverBlogResponse = toObject(responseBody, NaverBlogResponse.class);

        String blogUrlFormat = "https://blog.naver.com/%s/%s";

        for (NaverBlogResponse.Post post : naverBlogResponse.getPostList()) {

            String blogUrl = String.format(blogUrlFormat, blogId, post.getLogNo());

            list.add(new NaverPost(blogId, blogUrl, post.getTitle(), post.getAddDate()));
        }

        final int resultTotalCount = Integer.parseInt(naverBlogResponse.getTotalCount());

        return String.valueOf(resultTotalCount);
    }

    public RestResult getGpBlogKeywordRankings(MultiValueMap<String, String> param, Long memberSeq) throws Exception {
        String blogUrl = StringUtils.isNotBlank(param.get("blogUrl").get(0)) ? param.get("blogUrl").get(0) : "";
        String keyword = StringUtils.isNotBlank(param.get("keyword").get(0)) ? param.get("keyword").get(0) : "";

        if(StringUtils.isBlank(blogUrl) || StringUtils.isBlank(keyword)) {
            throw new Exception("검색 정보를 입력하세요.");
        }

        String baseUrl = "https://search.naver.com/search.naver?ssc=tab.blog.all&sm=tab_jum&query=" + keyword;
        int page = 1;
        int start = 1;
        int pagePerCount = 30;
        int ranking = 0;


        for(int i = 1; i < 3; i++) {
            String url = baseUrl + "&page=" + page + "&start=" + start;
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("referer", "https://www.naver.com/");

            addDefaultHeaders(headers);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            System.out.println("url: " + url);
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
            // 검색 결과 목록을 담고 있는 <ul> 태그를 찾음
            Element ulElement = doc.selectFirst("ul.lst_view._fe_view_infinite_scroll_append_target");

            // <li> 태그들 (각 검색 결과) 파싱
            if (ulElement != null) {
                Elements liElements = doc.select("li.bx");
                // 각 <li> 태그에서 <div class="user_box_inner"> 내부의 <a> 태그 추출
                for (Element li : liElements) {
                    Element userBoxInner = li.selectFirst("div.api_save_group a");
                    if (userBoxInner != null) {
                        URL dataUrl = new URL(userBoxInner.attr("data-url"));
                        if (dataUrl.equals(new URL(blogUrl))) {
                            Map<String, Object> data = new LinkedHashMap<>();
                            data.put("ranking", ranking + 1);
                            return new RestResult(data);
                        }
                    }
                    ranking++;
                }
            }
            start += pagePerCount;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ranking", 0);
        return new RestResult(data);
    }

    public static String encodeString(String input) {
        String encodedString = "";
        try {
            encodedString = URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodedString;
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
}
