package com.tbm.admin.service.thirdparty;

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

import java.util.ArrayList;
import java.util.List;

public class GpBlogRankingTests {

    @Test
    public void test() {
        String url = "https://blog.naver.com/PostList.naver?blogId=imvgw28841&widgetTypeCall=true&categoryNo=2&topReferer=https%3A%2F%2Fblog.naver.com%2FPostList.naver%3FblogId%3Dimvgw28841%26categoryNo%3D2&trackingCode=blog_bloghome&directAccess=true";

        // Create an instance of RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.add("accept-language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.add("cache-control", "no-cache");
        headers.add("cookie", "NNB=A6IQ6RD6RO7GG; ASID=7427d34200000185f8de087b0000004b; NV_WETR_LOCATION_RGN_M=\"MDI0NjMxMDI=\"; BA_DEVICE=cddf2ead-ee79-4b4a-bf76-831104de43ce; wcs_bt=1cde97b94dd8ef0:1695744685; _ga_451MFZ9CFM=GS1.1.1700319479.2.0.1700319479.0.0.0; _ga=GA1.2.1032842157.1674409295; _ga_LJ4WZ4013E=GS1.2.1702727846.1.1.1702727854.0.0.0; stat_yn=1; NFS=2; NV_WETR_LAST_ACCESS_RGN_M=\"MDI0NjMxMDI=\"; NAC=wQALBMwHhi1W; ba.uuid=df32031f-adf5-4635-b7a9-20d5e8d92478; NACT=1; page_uid=ioZPtsqVOsosslbCKFdssssst+l-332584; BUC=qvWHFtWwdTzgqkWT_dbE64VNYh4oIFGP5XvK6_Sd0eI=; JSESSIONID=8E9599D1A9D61EE4A3CA332E6D688B1B.jvm1");
        headers.add("pragma", "no-cache");
        headers.add("referer", "https://blog.naver.com/imvgw28841");
        headers.add("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        headers.add("sec-ch-ua-mobile", "?0");
        headers.add("sec-ch-ua-platform", "\"macOS\"");
        headers.add("sec-fetch-dest", "iframe");
        headers.add("sec-fetch-mode", "navigate");
        headers.add("sec-fetch-site", "same-origin");
        headers.add("upgrade-insecure-requests", "1");
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Send the request
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String html = response.getBody();

        // Parse the HTML
        Document doc = Jsoup.parse(html);
        Elements postElements = doc.select("li.item");

        List<BlogPost> blogPosts = new ArrayList<>();

        // Extract data
        for (Element postElement : postElements) {
            String title = postElement.select("strong.title.ell").text();
            String link = postElement.select("a.link.pcol2").attr("href");
            String date = postElement.select("span.date").text();

            if(title.isEmpty() || link.isEmpty() || date.isEmpty()) {
                continue;
            }
            blogPosts.add(new BlogPost(title, link, date));
        }

        // Print the results
        blogPosts.forEach(System.out::println);
    }

    static class BlogPost {
        private String title;
        private String link;
        private String date;

        public BlogPost(String title, String link, String date) {
            this.title = title;
            this.link = link;
            this.date = date;
        }

        @Override
        public String toString() {
            return "BlogPost{" +
                   "title='" + title + '\'' +
                   ", link='" + link + '\'' +
                   ", date='" + date + '\'' +
                   '}';
        }
    }
}
