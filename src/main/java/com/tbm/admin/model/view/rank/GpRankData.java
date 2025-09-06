package com.tbm.admin.model.view.rank;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GpRankData {
    private String keyword; // 검색할 키워드
    private int totalPage;  // 총 페이지 수
    private String grade;   // 등급
    private int ranking;    // 순위

    private List<String> referers = new ArrayList<>();
    private List<String> cookies = new ArrayList<>();
    private List<String> urls = new ArrayList<>();

    public String lastCookie() {
        return cookies.getLast();
    }

    public String lastReferer() {
        return referers.getLast();
    }

    public int extractRanking(String postingUrl) {

        if(urls.isEmpty()) return 0;

        int count = 0;
        for (String url : urls) {
            count++;
            if(url.equals(postingUrl)) {
                return count;
            }
        }
        return count;
    }
}
