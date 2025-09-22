package com.shinsegaelaw.admin.model.view.rank;

import lombok.Data;

@Data
public class NaverPost {

    private String blogId;
    private String url;
    private String title;
    private String postedAt;

    private String status;
    private int ranking;

    public NaverPost(String blogId, String url, String title, String postedAt) {
        this.blogId = blogId;
        this.url = url;
        this.title = title;
        this.postedAt = postedAt;
    }
}