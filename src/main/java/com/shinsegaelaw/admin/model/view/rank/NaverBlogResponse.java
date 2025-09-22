package com.shinsegaelaw.admin.model.view.rank;

import lombok.Data;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Data
public class NaverBlogResponse {
    private String resultCode;
    private String resultMessage;
    private List<Post> postList;
    private String countPerPage;
    private String totalCount;
    private String pagingHtml;
    private Parameters parameters;
    private Blog blog;
    private BlogUser blogUser;
    private String isTagEnabled;
    private String isTagEditable;
    private String isAllPostSelectable;
    private String tagQueryString;

    @Data
    public static class Post {
        private String sellerServiceStatus;
        private String logNo;
        private String title;
        private String categoryNo;
        private String parentCategoryNo;
        private String sourceCode;
        private String commentCount;
        private String readCount;
        private String addDate;
        private String openType;
        private String searchYn;
        private boolean greenReviewBannerYn;
        private String memologMovingYn;
        private String isPostSelectable;
        private String isPostNotOpen;
        private int isPostBlocked;
        private int isBlockTmpForced;
        private String postProductStatus;

        public String getTitle() {
            return URLDecoder.decode(title, StandardCharsets.UTF_8);
        }
    }

    @Data
    public static class Parameters {
        private String listType;
        private String currentPage;
        private String countPerPage;
        private String categoryNo;
        private String parentCategoryNo;
        private String viewDate;
        private String logNo;
    }

    @Data
    public static class Blog {
        private String blogId;
        private String blogNo;
    }

    @Data
    public static class BlogUser {
        private String isBlogOwner;
    }
}