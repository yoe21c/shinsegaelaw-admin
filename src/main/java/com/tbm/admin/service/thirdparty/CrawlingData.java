package com.tbm.admin.service.thirdparty;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CrawlingData {

    private String phpSessionIdCookie;
    private String crxpblogmatrixCookie;
    private String csrfToken;
    private String authenticationCode;
    private List<PostDetail> postDetails = new ArrayList<>();
    private List<PostDetail> totalPostDetails = new ArrayList<>();
    private int totalPages;
    private String userInfo;
    private int usedCount;

    // 파라미터
    private String blogId;
}
