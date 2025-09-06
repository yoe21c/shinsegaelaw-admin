package com.tbm.admin.service.thirdparty;

import lombok.Data;

@Data
public class PostDetail {
    private final String date;
    private final String title;
    private final String url;
    private final String dataSize;
    private String result;    // 마지막에 이 값을 채운다.
}