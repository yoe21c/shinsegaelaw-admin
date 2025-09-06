package com.tbm.admin.model.view.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown=true) // ignore unknown properties when deserialize
@JsonInclude(JsonInclude.Include.NON_NULL) // not null only
public class RestResult {

    @JsonIgnore
    private boolean success = false;

    // error id
    private String id;

    // error message
    private String message;

    private Map<String, Object> data;

    private List<?> list;

    private String url;     // ckeditor 5 용으로 url 을 리턴하고 에디터 <img src="여기에 표기하기 위해서"> 에 표기

    public RestResult() { }

    public RestResult(String url) {
        this.url = url;
    }

    public RestResult(Map<String, Object> data) {
        this.data = data;
    }

    public RestResult setData(Map<String, Object> data) {

        this.data = data;
        return this;
    }

    public RestResult setList(List<?> list) {

        this.list = list;
        return this;
    }

    public boolean isSuccess() {
        Map<String, Object> data = (Map) getData();
        return success || (data != null && data.get("result") != null && data.get("result").equals("ok"));
    }

    public static RestResult success() {
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", "ok");
        restResult.setData(map);

        return restResult;
    }

    public static RestResult notOk() {
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", "notOk");
        restResult.setData(map);

        return restResult;
    }

    public static RestResult failure() {
        RestResult restResult = new RestResult();
        restResult.setSuccess(false);
        restResult.setId("server_error");
        return restResult;
    }

}